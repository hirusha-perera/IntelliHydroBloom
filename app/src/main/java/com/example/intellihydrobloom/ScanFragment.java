package com.example.intellihydrobloom;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class ScanFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    //private SimpleMatrix W_conv;
    private List<SimpleMatrix> W_conv_list;
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

    public List<SimpleMatrix> convolution(SimpleMatrix image, List<SimpleMatrix> filters, double bias) {
        List<SimpleMatrix> outputList = new ArrayList<>();
        for (SimpleMatrix filter : filters) {
            int outputSize = image.numRows() - filter.numRows() + 1;
            SimpleMatrix output = new SimpleMatrix(outputSize, outputSize);
            for (int i = 0; i < outputSize; i++) {
                for (int j = 0; j < outputSize; j++) {
                    SimpleMatrix subMatrix = image.extractMatrix(i, i + filter.numRows(), j, j + filter.numCols());
                    double value = subMatrix.elementMult(filter).elementSum() + bias;
                    output.set(i, j, value);
                }
            }
            outputList.add(output);
        }
        return outputList;
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


    private SimpleMatrix fullyConnected(SimpleMatrix input, SimpleMatrix weights, SimpleMatrix biases) {
        Log.d("ScanFragment", "Input matrix dimensions: " + input.numRows() + "x" + input.numCols());
        Log.d("ScanFragment", "Weights matrix dimensions: " + weights.numRows() + "x" + weights.numCols());
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
        StorageReference modelRef = storage.getReferenceFromUrl("https://firebasestorage.googleapis.com/v0/b/intelli-hydro-bloom-7b31f.appspot.com/o/IHB_Plantdis.json?alt=media&token=5b070147-e5cf-42dd-b6b9-fd4f320555ca");

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
            W_conv_list = new ArrayList<>();
            for (int i = 0; i < W_convJSON.length(); i++) {
                JSONArray subArray = W_convJSON.getJSONArray(i);
                double[][] W_conv2D = new double[subArray.length()][];
                for (int j = 0; j < subArray.length(); j++) {
                    JSONArray subSubArray = subArray.getJSONArray(j);
                    W_conv2D[j] = new double[subSubArray.length()];
                    for (int k = 0; k < subSubArray.length(); k++) {
                        W_conv2D[j][k] = subSubArray.getDouble(k);
                    }
                }
                W_conv_list.add(new SimpleMatrix(W_conv2D));
            };

            // Extracting b_conv
            JSONArray b_convJSON = modelParams.getJSONArray("b_conv");
            double[][] b_conv_temp = new double[b_convJSON.length()][1];  // It's a 2D array
            for (int i = 0; i < b_convJSON.length(); i++) {
                b_conv_temp[i][0] = b_convJSON.getJSONArray(i).getDouble(0);
            }
            this.b_conv = new SimpleMatrix(b_conv_temp);

            // Extracting W_fc
            JSONArray W_fcJSON = modelParams.getJSONArray("W_fc");
            double[][] W_fc_temp = new double[W_fcJSON.length()][];
            for (int i = 0; i < W_fcJSON.length(); i++) {
                JSONArray subArray = W_fcJSON.getJSONArray(i);
                W_fc_temp[i] = new double[subArray.length()];
                for (int j = 0; j < subArray.length(); j++) {
                    W_fc_temp[i][j] = subArray.getDouble(j);
                }
            }
            this.W_fc = new SimpleMatrix(W_fc_temp);

            // Extracting b_fc
            JSONArray b_fcJSON = modelParams.getJSONArray("b_fc");
            double[][] b_fc_temp = new double[b_fcJSON.length()][1];  // It's a 2D array
            for (int i = 0; i < b_fcJSON.length(); i++) {
                b_fc_temp[i][0] = b_fcJSON.getJSONArray(i).getDouble(0);
            }
            this.b_fc = new SimpleMatrix(b_fc_temp);

            // Model parameters are now initialized and ready for use.

        } catch (Exception e) {
            // Handle exceptions
            Log.e("ScanFragment", "Error parsing model: " + e.getMessage());
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

        Log.d("ScanFragment", "Inside onActivityResult");

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_IMAGE_CAPTURE: {
                    Log.d("ScanFragment", "Image captured");
                    Bundle extras = data.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    ivPlantImage.setImageBitmap(imageBitmap);
                    preprocessAndFeedToNN(imageBitmap);
                    break;
                }
                case REQUEST_PICK_IMAGE: {
                    Log.d("ScanFragment", "Image picked from gallery");
                    Uri imageUri = data.getData();
                    try {
                        Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
                        ivPlantImage.setImageBitmap(imageBitmap);
                        preprocessAndFeedToNN(imageBitmap);
                    } catch (Exception e) {
                        Log.d("ScanFragment", "Error processing picked image: " + e.getMessage());
                        e.printStackTrace();
                    }
                    break;
                }
            }
        } else {
            Log.d("ScanFragment", "Result not OK");
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
        // Check for null matrices
        if (W_conv_list == null || b_conv == null || W_fc == null || b_fc == null) {
            Log.e("ScanFragment", "Neural network parameters are not initialized");
            return null;
        }

        // Implement the series of operations to get the neural network's output
        List<SimpleMatrix> convOutputs = convolution(input, W_conv_list, b_conv.get(0, 0));

        // Log Convolutional Outputs dimensions
        for (int i = 0; i < convOutputs.size(); i++) {
            Log.d("ScanFragment", "Convolutional Output " + (i+1) + " dimensions: " + convOutputs.get(i).numRows() + "x" + convOutputs.get(i).numCols());
        }

        List<SimpleMatrix> reluOutputs = new ArrayList<>();
        for (SimpleMatrix convOutput : convOutputs) {
            reluOutputs.add(relu(convOutput));
        }

        // Log ReLU Outputs dimensions
        for (int i = 0; i < reluOutputs.size(); i++) {
            Log.d("ScanFragment", "ReLU Output " + (i+1) + " dimensions: " + reluOutputs.get(i).numRows() + "x" + reluOutputs.get(i).numCols());
        }

        List<SimpleMatrix> pooledOutputs = new ArrayList<>();
        for (SimpleMatrix reluOutput : reluOutputs) {
            pooledOutputs.add(maxPooling(reluOutput, POOL_SIZE, POOL_SIZE));
        }

        // Log Pooling Outputs dimensions
        for (int i = 0; i < pooledOutputs.size(); i++) {
            Log.d("ScanFragment", "Pooling Output " + (i+1) + " dimensions: " + pooledOutputs.get(i).numRows() + "x" + pooledOutputs.get(i).numCols());
        }

        // Flatten and concatenate the pooled outputs
        List<SimpleMatrix> flattenedOutputs = new ArrayList<>();
        for (SimpleMatrix pooledOutput : pooledOutputs) {
            flattenedOutputs.add(flattenMatrix(pooledOutput));
        }

        SimpleMatrix firstMatrix = flattenedOutputs.get(0);
        // Start concatenation from the second matrix
        SimpleMatrix concatenatedOutput = flattenedOutputs.get(0);
        for (int i = 1; i < flattenedOutputs.size(); i++) {
            concatenatedOutput = concatenatedOutput.concatRows(flattenedOutputs.get(i));
        }


        // Log dimensions after Flattening and Concatenation
        Log.d("ScanFragment", "Concatenated Output dimensions: " + concatenatedOutput.numRows() + "x" + concatenatedOutput.numCols());

        SimpleMatrix fcOutput = fullyConnected(concatenatedOutput, W_fc, b_fc);

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
        predictionTextView.setText("." + categories[predictedClassIndex]);
    }
    private void preprocessAndFeedToNN(Bitmap imageBitmap) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, IMG_SIZE, IMG_SIZE, false);

        // Convert the image to a matrix format
        SimpleMatrix inputMatrix = convertBitmapToMatrix(resizedBitmap);

        // Feed to neural network and get the result
        SimpleMatrix result = forwardPass(inputMatrix);

        if (result != null) {
            predictedCategory = getPredictedCategory(result);
            // Display the prediction in a TextView
            TextView predictionTextView = getView().findViewById(R.id.tv_dis);
            predictionTextView.setText("" + predictedCategory);
        } else {
            // Handle the error by showing a message to the user
            Toast.makeText(requireContext(), "Error processing the image. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private String getPredictedCategory(SimpleMatrix fcOutput) {
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

        // Return the prediction
        return categories[predictedClassIndex];
    }
    private String predictedCategory;

    private void uploadImageToFirebase() {
        // Convert the image in ImageView to Bitmap
        Bitmap bitmap = ((BitmapDrawable) ivPlantImage.getDrawable()).getBitmap();

        // Convert the Bitmap to ByteArrayOutputStream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        // Firebase storage instance
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        // Create a reference to "uploadedImages/predictedCategory.jpg"
        // (replace spaces and special characters to avoid issues with file names)
        String imageName = predictedCategory.replaceAll("[^a-zA-Z0-9]", "_") + ".jpg";
        StorageReference imageRef = storageRef.child("uploadedImages/" + imageName);

        // Upload the byte array
        imageRef.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> {
                    // Handle successful uploads on complete
                    Toast.makeText(requireContext(), "Image uploaded successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(exception -> {
                    // Handle unsuccessful uploads
                    Toast.makeText(requireContext(), "Error in uploading image", Toast.LENGTH_SHORT).show();
                });
    }



}


