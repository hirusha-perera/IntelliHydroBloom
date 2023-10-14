package com.example.intellihydrobloom;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.ejml.simple.SimpleMatrix;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;

public class ScanFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    private SimpleMatrix W_conv;
    private SimpleMatrix b_conv;
    private SimpleMatrix W_fc;
    private SimpleMatrix b_fc;
    private ImageView ivPlantImage;
    private Button btnCapture, btnPickFromGallery;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;
    private static final int PERMISSIONS_REQUEST_CAMERA = 1001;
    private static final int PERMISSIONS_REQUEST_STORAGE = 1002;

    public ScanFragment() {
        // Required empty public constructor
    }

    public static SimpleMatrix convolution(SimpleMatrix image, SimpleMatrix filter, double bias) {
        int outputSize = image.numRows() - filter.numRows() + 1;
        SimpleMatrix output = new SimpleMatrix(outputSize, outputSize);
        for (int i = 0; i < outputSize; i++) {
            for (int j = 0; j < outputSize; j++) {
                SimpleMatrix subMatrix = image.extractMatrix(i, i + filter.numRows(), j, j + filter.numCols());
                double value = subMatrix.elementMult(filter).elementSum() + bias;
                output.set(i, j, value);
            }
        }


        return output;
    }
    public static SimpleMatrix relu(SimpleMatrix input) {
        for (int i = 0; i < input.numRows(); i++) {
            for (int j = 0; j < input.numCols(); j++) {
                double value = Math.max(0, input.get(i, j));
                input.set(i, j, value);
            }
        }
        return input;
    }

    public static SimpleMatrix maxPooling(SimpleMatrix input, int poolSize, int stride) {
        int outputSize = (input.numRows() - poolSize) / stride + 1;
        SimpleMatrix output = new SimpleMatrix(outputSize, outputSize);
        for (int i = 0; i < outputSize; i++) {
            for (int j = 0; j < outputSize; j++) {
                SimpleMatrix subMatrix = input.extractMatrix(i * stride, i * stride + poolSize, j * stride, j * stride + poolSize);

                // Manually find the max value in the subMatrix
                double maxValue = Double.NEGATIVE_INFINITY;
                for (int x = 0; x < subMatrix.numRows(); x++) {
                    for (int y = 0; y < subMatrix.numCols(); y++) {
                        if (subMatrix.get(x, y) > maxValue) {
                            maxValue = subMatrix.get(x, y);
                        }
                    }
                }
                output.set(i, j, maxValue);
            }
        }
        return output;
    }


    public static SimpleMatrix fullyConnected(SimpleMatrix input, SimpleMatrix weights, SimpleMatrix biases) {
        return weights.mult(input).plus(biases);
    }




    public static ScanFragment newInstance(String param1, String param2) {
        ScanFragment fragment = new ScanFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        // Download and parse the model
        downloadModel();
    }

    private void downloadModel() {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference modelRef = storage.getReferenceFromUrl("https://firebasestorage.googleapis.com/v0/b/intelli-hydro-bloom-7b31f.appspot.com/o/model_params1.json?alt=media&token=1502bb7e-089c-4096-99c2-41b2dc3c71fb");

        try {
            File localFile = File.createTempFile("model", "json");

            modelRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    // Model downloaded successfully
                    parseAndUseModel(localFile);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle any errors
                }
            });
        } catch (Exception e) {
            // Handle exceptions
        }
    }

    private void parseAndUseModel(File modelFile) {
        try {
            FileInputStream fis = new FileInputStream(modelFile);
            byte[] data = new byte[(int) modelFile.length()];
            fis.read(data);
            fis.close();

            String jsonString = new String(data, "UTF-8");
            JSONObject modelParams = new JSONObject(jsonString);

            // Extracting W_conv
            JSONArray W_convJSON = modelParams.getJSONArray("W_conv");
            double[][][] W_conv = new double[W_convJSON.length()][][];
            for (int i = 0; i < W_convJSON.length(); i++) {
                JSONArray subArray = W_convJSON.getJSONArray(i);
                W_conv[i] = new double[subArray.length()][];
                for (int j = 0; j < subArray.length(); j++) {
                    JSONArray subSubArray = subArray.getJSONArray(j);
                    W_conv[i][j] = new double[subSubArray.length()];
                    for (int k = 0; k < subSubArray.length(); k++) {
                        W_conv[i][j][k] = subSubArray.getDouble(k);
                    }
                }
            }

            // Extracting b_conv
            JSONArray b_convJSON = modelParams.getJSONArray("b_conv");
            double[][] b_conv = new double[b_convJSON.length()][1];  // It's a 2D array
            for (int i = 0; i < b_convJSON.length(); i++) {
                b_conv[i][0] = b_convJSON.getJSONArray(i).getDouble(0);
            }

            // Extracting W_fc
            JSONArray W_fcJSON = modelParams.getJSONArray("W_fc");
            double[][] W_fc = new double[W_fcJSON.length()][];
            for (int i = 0; i < W_fcJSON.length(); i++) {
                JSONArray subArray = W_fcJSON.getJSONArray(i);
                W_fc[i] = new double[subArray.length()];
                for (int j = 0; j < subArray.length(); j++) {
                    W_fc[i][j] = subArray.getDouble(j);
                }
            }

            // Extracting b_fc
            JSONArray b_fcJSON = modelParams.getJSONArray("b_fc");
            double[][] b_fc = new double[b_fcJSON.length()][1];  // It's a 2D array
            for (int i = 0; i < b_fcJSON.length(); i++) {
                b_fc[i][0] = b_fcJSON.getJSONArray(i).getDouble(0);
            }

            // You can now use these arrays in your model operations.

        } catch (Exception e) {
            // Handle exceptions
        }
    }




    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_scan, container, false);

        ivPlantImage = rootView.findViewById(R.id.iv_plant_image);
        btnCapture = rootView.findViewById(R.id.btn_capture);
        btnPickFromGallery = rootView.findViewById(R.id.btn_pick_from_gallery);

        btnCapture.setOnClickListener(v -> captureImage());
        btnPickFromGallery.setOnClickListener(v -> pickImageFromGallery());

        return rootView;
    }

    private void captureImage() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CAMERA);
        } else {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private void pickImageFromGallery() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_STORAGE);
        } else {
            Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(pickPhotoIntent, REQUEST_PICK_IMAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CAMERA: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    captureImage();
                } else {
                    Toast.makeText(requireContext(), "Camera permission is required to capture image", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case PERMISSIONS_REQUEST_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickImageFromGallery();
                } else {
                    Toast.makeText(requireContext(), "Storage permission is required to pick image", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_IMAGE_CAPTURE: {
                    Bundle extras = data.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    ivPlantImage.setImageBitmap(imageBitmap);
                    preprocessAndFeedToNN(imageBitmap);
                    break;
                }
                case REQUEST_PICK_IMAGE: {
                    Uri imageUri = data.getData();
                    try {
                        Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
                        ivPlantImage.setImageBitmap(imageBitmap);
                        preprocessAndFeedToNN(imageBitmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
    }
    private SimpleMatrix reshape(SimpleMatrix matrix, int rows, int cols) {
        SimpleMatrix reshapedMatrix = new SimpleMatrix(rows, cols);
        int count = 0;
        for (int i = 0; i < matrix.numRows(); i++) {
            for (int j = 0; j < matrix.numCols(); j++) {
                reshapedMatrix.set(count / cols, count % cols, matrix.get(i, j));
                count++;
            }
        }
        return reshapedMatrix;
    }

    private SimpleMatrix convertBitmapToMatrix(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        SimpleMatrix matrix = new SimpleMatrix(width, height);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int pixelValue = bitmap.getPixel(i, j);
                double grayValue = 0.3 * Color.red(pixelValue) + 0.59 * Color.green(pixelValue) + 0.11 * Color.blue(pixelValue);
                matrix.set(i, j, grayValue / 255.0);  // Normalize the grayscale value
            }
        }
        return matrix;
    }


    private SimpleMatrix flattenMatrix(SimpleMatrix matrix) {
        int rows = matrix.numRows();
        int cols = matrix.numCols();
        SimpleMatrix flattened = new SimpleMatrix(rows * cols, 1);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                flattened.set(i * cols + j, 0, matrix.get(i, j));
            }
        }
        return flattened;
    }



    private static final int IMG_SIZE = 64;
    private static final int POOL_SIZE = 2;

    private String[] categories = {"Caterpillar attacks", "Healthy", "Nutrient deficiency", "Leaf spots", "Mealybugs", "Leaf spots"};

    private SimpleMatrix forwardPass(SimpleMatrix input) {
        // Implement the series of operations to get the neural network's output

        SimpleMatrix convOutput = convolution(input, W_conv, b_conv.get(0, 0)); // Assuming W_conv is a SimpleMatrix
        SimpleMatrix reluOutput = relu(convOutput);
        SimpleMatrix pooledOutput = maxPooling(reluOutput, POOL_SIZE, POOL_SIZE);
        SimpleMatrix flattenedOutput = flattenMatrix(pooledOutput);
        SimpleMatrix fcOutput = fullyConnected(flattenedOutput, W_fc, b_fc); // Assuming W_fc and b_fc are SimpleMatrices

        return fcOutput;
    }

    private void displayPrediction(SimpleMatrix fcOutput) {
        // Find the index of the maximum value in fcOutput
        int predictedClassIndex = 0;
        double maxVal = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < fcOutput.numRows(); i++) {
            double val = fcOutput.get(i, 0);
            if (val > maxVal) {
                maxVal = val;
                predictedClassIndex = i;
            }
        }

        // Display the prediction in a TextView
        TextView predictionTextView = getView().findViewById(R.id.tv_dis);
        predictionTextView.setText("This Plant Have " + categories[predictedClassIndex]);
    }
    private void preprocessAndFeedToNN(Bitmap imageBitmap) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, IMG_SIZE, IMG_SIZE, false);

        // Convert the image to a matrix format
        SimpleMatrix inputMatrix = convertBitmapToMatrix(resizedBitmap);

        // Feed to neural network and get the result
        SimpleMatrix result = forwardPass(inputMatrix);


        displayPrediction(result);

    }
}


