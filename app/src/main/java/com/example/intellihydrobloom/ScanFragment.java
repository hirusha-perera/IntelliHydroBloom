package com.example.intellihydrobloom;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
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
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.ejml.simple.SimpleMatrix;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ScanFragment extends Fragment {


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

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        downloadModel();
    }
    private void downloadModel() {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference modelRef = storage.getReferenceFromUrl("https://firebasestorage.googleapis.com/v0/b/intelli-hydro-bloom-7b31f.appspot.com/o/IHB_Plantdispest.json?alt=media&token=7267c68e-86bb-40ca-ae38-c4caae456ec3");

        try {
            File localFile = File.createTempFile("model", "json");

            modelRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {

                    parseAndUseModel(localFile);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {

                }
            });
        } catch (Exception e) {

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void parseAndUseModel(File modelFile) {
        try {
            String jsonString = new String(Files.readAllBytes(modelFile.toPath()), "UTF-8");
            JSONObject modelParams = new JSONObject(jsonString);

            W_conv_list = parse2DJSONArrayToSimpleMatrixList(modelParams.getJSONArray("W_conv"));
            b_conv = parse1DJSONArrayToSimpleMatrix(modelParams.getJSONArray("b_conv"));
            W_fc = parse2DJSONArrayToSimpleMatrix(modelParams.getJSONArray("W_fc"));
            b_fc = parse1DJSONArrayToSimpleMatrix(modelParams.getJSONArray("b_fc"));

        } catch (Exception e) {
            Log.e("ScanFragment", "Error parsing model: " + e.getMessage());
        }
    }

    private List<SimpleMatrix> parse2DJSONArrayToSimpleMatrixList(JSONArray jsonArray) throws JSONException {
        List<SimpleMatrix> matrixList = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            matrixList.add(parse2DJSONArrayToSimpleMatrix(jsonArray.getJSONArray(i)));
        }
        return matrixList;
    }

    private SimpleMatrix parse1DJSONArrayToSimpleMatrix(JSONArray jsonArray) throws JSONException {
        double[][] matrixData = new double[jsonArray.length()][1];
        for (int i = 0; i < jsonArray.length(); i++) {
            matrixData[i][0] = jsonArray.getJSONArray(i).getDouble(0);
        }
        return new SimpleMatrix(matrixData);
    }

    private SimpleMatrix parse2DJSONArrayToSimpleMatrix(JSONArray jsonArray) throws JSONException {
        double[][] matrixData = new double[jsonArray.length()][];
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONArray subArray = jsonArray.getJSONArray(i);
            matrixData[i] = new double[subArray.length()];
            for (int j = 0; j < subArray.length(); j++) {
                matrixData[i][j] = subArray.getDouble(j);
            }
        }
        return new SimpleMatrix(matrixData);
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

    static SimpleMatrix reshape(SimpleMatrix matrix, int rows, int cols) {
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

    SimpleMatrix flattenMatrix(SimpleMatrix matrix) {
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

    private String[] categories = {"Not a Plant", "Leaf Curl", "Leaf Spots", "Caterpillar Attack", "Healthy", "Mealy Bugs", "Leaf Eating Ladybird Attack", "Nutrients Defficiency"};

    private SimpleMatrix forwardPass(SimpleMatrix input) {
        if (W_conv_list == null || b_conv == null || W_fc == null || b_fc == null) {
            Log.e("ScanFragment", "Neural network parameters are not initialized");
            return null;
        }

        List<SimpleMatrix> convOutputs = performConvolution(input);
        List<SimpleMatrix> reluOutputs = applyReLU(convOutputs);
        List<SimpleMatrix> pooledOutputs = performMaxPooling(reluOutputs);
        SimpleMatrix concatenatedOutput = concatenateAndFlatten(pooledOutputs);

        Log.d("ScanFragment", "Concatenated Output dimensions: " + concatenatedOutput.numRows() + "x" + concatenatedOutput.numCols());

        return fullyConnected(concatenatedOutput, W_fc, b_fc);
    }

    private List<SimpleMatrix> performConvolution(SimpleMatrix input) {
        return convolution(input, W_conv_list, b_conv.get(0, 0));
    }

    private List<SimpleMatrix> applyReLU(List<SimpleMatrix> convOutputs) {
        List<SimpleMatrix> reluOutputs = new ArrayList<>();
        for (SimpleMatrix convOutput : convOutputs) {
            reluOutputs.add(relu(convOutput));
        }
        return reluOutputs;
    }

    private List<SimpleMatrix> performMaxPooling(List<SimpleMatrix> reluOutputs) {
        List<SimpleMatrix> pooledOutputs = new ArrayList<>();
        for (SimpleMatrix reluOutput : reluOutputs) {
            pooledOutputs.add(maxPooling(reluOutput, POOL_SIZE, POOL_SIZE));
        }
        return pooledOutputs;
    }

    private SimpleMatrix concatenateAndFlatten(List<SimpleMatrix> pooledOutputs) {
        List<SimpleMatrix> flattenedOutputs = new ArrayList<>();
        for (SimpleMatrix pooledOutput : pooledOutputs) {
            flattenedOutputs.add(flattenMatrix(pooledOutput));
        }

        SimpleMatrix concatenatedOutput = flattenedOutputs.get(0);
        for (int i = 1; i < flattenedOutputs.size(); i++) {
            concatenatedOutput = concatenatedOutput.concatRows(flattenedOutputs.get(i));
        }
        return concatenatedOutput;
    }

    private void preprocessAndFeedToNN(Bitmap imageBitmap) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, IMG_SIZE, IMG_SIZE, false);

        SimpleMatrix inputMatrix = convertBitmapToMatrix(resizedBitmap);

        SimpleMatrix result = forwardPass(inputMatrix);

        if (result != null) {
            predictedCategory = getPredictedCategory(result);

            TextView predictionTextView = getView().findViewById(R.id.tv_dis);
            predictionTextView.setText("" + predictedCategory);
        } else {

            Toast.makeText(requireContext(), "Error processing the image. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private String getPredictedCategory(SimpleMatrix fcOutput) {

        int predictedClassIndex = 0;
        double maxVal = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < fcOutput.numRows(); i++) {
            double val = fcOutput.get(i, 0);
            if (val > maxVal) {
                maxVal = val;
                predictedClassIndex = i;
            }
        }


        return categories[predictedClassIndex];
    }
    private String predictedCategory;
    private void uploadImageToFirebase() {

        Bitmap bitmap = ((BitmapDrawable) ivPlantImage.getDrawable()).getBitmap();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        String imageName = predictedCategory.replaceAll("[^a-zA-Z0-9]", "_") + ".jpg";
        StorageReference imageRef = storageRef.child("uploadedImages/" + imageName);

        imageRef.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> {

                    Toast.makeText(requireContext(), "Image uploaded successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(exception -> {

                    Toast.makeText(requireContext(), "Error in uploading image", Toast.LENGTH_SHORT).show();
                });
    }

}


