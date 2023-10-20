#!/usr/bin/env python
# coding: utf-8

# In[1]:


import pandas as pd
import numpy as np
import matplotlib.pyplot as plt


# In[2]:


data_df = pd.read_csv(r"C:\Users\ASUS\Desktop\Intelli\Plant_Diseases_Pests_shuf1.csv")


# In[3]:


data_df.head()


# In[4]:


missing_values = data_df.isnull().sum().sum()

class_distribution = data_df['label'].value_counts()

missing_values, class_distribution


# In[15]:


num_samples = 5

samples = data_df.sample(num_samples)

fig, axes = plt.subplots(1, num_samples, figsize=(15, 15))

for i, (index, row) in enumerate(samples.iterrows()):
    image_data = row[:-1].astype(float).values.reshape(64, 64)
    label = row['label']
    axes[i].imshow(image_data, cmap='gray')
    axes[i].set_title(label)
    axes[i].axis('off')

plt.tight_layout()
plt.show()


# In[5]:


plt.figure(figsize=(12, 6))
class_distribution.plot(kind='bar', color='skyblue')
plt.title('Distribution of Target Classes')
plt.ylabel('Number of Samples')
plt.xlabel('Class Labels')
plt.xticks(rotation=45)
plt.grid(axis='y')
plt.tight_layout()
plt.show()


# In[16]:


unique_labels = data_df["label"].unique()
unique_labels


# In[17]:


from sklearn.model_selection import train_test_split
import numpy as np

# Adjust categories based on unique labels from the dataset
categories = list(unique_labels)

# Convert labels to one-hot encoding 
data_df["label_index"] = data_df["label"].apply(categories.index)
labels_one_hot = np.eye(len(categories))[data_df["label_index"].values]

# Split data into training and validation sets 
train_data, val_data, train_labels, val_labels = train_test_split(
    data_df.drop(columns=["label", "label_index"]).values,
    labels_one_hot,
    test_size=0.2,
    random_state=42
)

train_data.shape, val_data.shape, train_labels.shape, val_labels.shape


# In[18]:


input_size = 64 * 64       # Flattened image size
num_filters = 8            # Number of filters in the convolutional layer
filter_size = 3            # Size of each filter (3x3)
pool_size = 2              # Size of pooling filter (2x2)
stride = 1                 # Stride for convolution
output_neurons = len(categories)  # Number of neurons in the output layer
IMG_SIZE = 64


# In[19]:


conv_output_size = (IMG_SIZE - filter_size) // stride + 1
pool_output_size = conv_output_size // pool_size
flattened_pool_output_size = num_filters * pool_output_size * pool_output_size
np.random.seed(42)  

W_conv = np.random.randn(num_filters, filter_size, filter_size) * 0.01
b_conv = np.zeros((num_filters, 1))
W_fc = np.random.randn(output_neurons, flattened_pool_output_size) * 0.01
b_fc = np.zeros((output_neurons, 1))

W_conv.shape, b_conv.shape, W_fc.shape, b_fc.shape


# In[20]:


def convolution(image, filters, biases, stride=1):
  
    filter_size = filters.shape[1]
    output_size = (image.shape[1] - filter_size) // stride + 1
    
    output = np.zeros((filters.shape[0], output_size, output_size))
    
    for f in range(filters.shape[0]):
        for i in range(0, output_size, stride):
            for j in range(0, output_size, stride):
                output[f, i, j] = np.sum(image[:, i:i+filter_size, j:j+filter_size] * filters[f]) + biases[f]
                
    return output

def relu(x):

    return np.maximum(0, x)

def max_pooling(x, pool_size, stride):

    output_size = x.shape[1] // pool_size
    output = np.zeros((x.shape[0], output_size, output_size))
    
    for i in range(0, output_size, stride):
        for j in range(0, output_size, stride):
            output[:, i, j] = np.max(x[:, i*pool_size:(i+1)*pool_size, j*pool_size:(j+1)*pool_size], axis=(1,2))
            
    return output

def fully_connected(x, weights, biases):
 
    return np.dot(weights, x) + biases


# In[21]:


sample_image = train_data[0].reshape(1, IMG_SIZE, IMG_SIZE)

# Convolution
conv_output = convolution(sample_image, W_conv, b_conv)
# ReLU Activation
relu_output = relu(conv_output)
# Max Pooling
pooled_output = max_pooling(relu_output, pool_size, stride=pool_size)
# Flatten Pooled Output
flattened_output = pooled_output.flatten().reshape(-1, 1)
# Fully Connected Layer
fc_output = fully_connected(flattened_output, W_fc, b_fc)

fc_output


# In[22]:


def mse_loss_gradient(predictions, labels):
 
    return predictions - labels

def fully_connected_gradient(dloss, activations_prev):
  
    dW = np.dot(dloss, activations_prev.T)
    db = dloss
    return dW, db

def relu_gradient(dloss, activations):

    return dloss * (activations > 0)


# In[23]:


dloss_fc = mse_loss_gradient(fc_output, train_labels[0].reshape(-1, 1))

dW_fc, db_fc = fully_connected_gradient(dloss_fc, flattened_output)

dloss_relu = relu_gradient(np.dot(W_fc.T, dloss_fc), pooled_output.flatten().reshape(-1, 1))


dW_fc.shape, db_fc.shape, dloss_relu.shape

def max_pooling_gradient(dloss, original_activations, pool_size, stride):
  
    output = np.zeros_like(original_activations)
    for i in range(0, dloss.shape[1], stride):
        for j in range(0, dloss.shape[2], stride):
            patch = original_activations[:, i*pool_size:(i+1)*pool_size, j*pool_size:(j+1)*pool_size]
            for k in range(dloss.shape[0]):
                i_max, j_max = np.unravel_index(patch[k].argmax(), patch[k].shape)
                output[k, i*pool_size + i_max, j*pool_size + j_max] = dloss[k, i, j]
    return output

dloss_pool = max_pooling_gradient(dloss_relu.reshape(pooled_output.shape), relu_output, pool_size, stride=pool_size)
dloss_pool.shape


# In[24]:


def convolution_gradient(dloss, original_image, filters, stride=1):
 
    filter_size = filters.shape[1]
    dW = np.zeros_like(filters)
    db = np.sum(dloss, axis=(1,2)).reshape(-1, 1)
    
    for f in range(filters.shape[0]):
        for i in range(0, filter_size):
            for j in range(0, filter_size):
                dW[f, i, j] = np.sum(original_image[:, i:i+dloss.shape[1]*stride:stride, j:j+dloss.shape[2]*stride:stride] * dloss[f])
                
    return dW, db

dW_conv, db_conv = convolution_gradient(dloss_pool, sample_image, W_conv)

dW_conv.shape, db_conv.shape

def update_weights_biases(W, b, dW, db, learning_rate):
 
    W -= learning_rate * dW
    b -= learning_rate * db
    return W, b


# In[29]:


import cv2

def augment_data(image):
    
    angle = np.random.uniform(-15, 15)
    
    center = (image.shape[1] / 2, image.shape[0] / 2)
    
    rotation_matrix = cv2.getRotationMatrix2D(center, angle, 1)
    
    rotated_image = cv2.warpAffine(image, rotation_matrix, (image.shape[1], image.shape[0]))
    
    return rotated_image


# In[32]:


learning_rate = 0.001
num_epochs = 30

for epoch in range(num_epochs):
    total_loss = 0
    for i in range(len(train_data)):

        image = train_data[i].reshape(1, IMG_SIZE, IMG_SIZE)
        
        label = train_labels[i].reshape(-1, 1)
        
        conv_output = convolution(image, W_conv, b_conv)
        relu_output = relu(conv_output)
        pooled_output = max_pooling(relu_output, pool_size, stride=pool_size)
        flattened_output = pooled_output.flatten().reshape(-1, 1)
        fc_output = fully_connected(flattened_output, W_fc, b_fc)
        
        loss = 0.5 * np.sum((fc_output - label)**2)
        total_loss += loss
        dloss_fc = mse_loss_gradient(fc_output, label)
        
        dW_fc, db_fc = fully_connected_gradient(dloss_fc, flattened_output)
        dloss_relu = relu_gradient(np.dot(W_fc.T, dloss_fc), pooled_output.flatten().reshape(-1, 1))
        dloss_pool = max_pooling_gradient(dloss_relu.reshape(pooled_output.shape), relu_output, pool_size, stride=pool_size)
        dW_conv, db_conv = convolution_gradient(dloss_pool, image, W_conv)
        
        W_fc, b_fc = update_weights_biases(W_fc, b_fc, dW_fc, db_fc, learning_rate)
        W_conv, b_conv = update_weights_biases(W_conv, b_conv, dW_conv, db_conv, learning_rate)
    
    print(f"Epoch {epoch+1}/{num_epochs} - Loss: {total_loss/len(train_data)}")
    
W_fc, b_fc, W_conv, b_conv


# In[23]:


def predict_image(image, W_conv, b_conv, W_fc, b_fc):
 
    # Forward pass
    conv_output = convolution(image, W_conv, b_conv)
    relu_output = relu(conv_output)
    pooled_output = max_pooling(relu_output, pool_size, stride=pool_size)
    flattened_output = pooled_output.flatten().reshape(-1, 1)
    fc_output = fully_connected(flattened_output, W_fc, b_fc)
    
    # Return the index of the maximum value as the predicted class
    return np.argmax(fc_output)

# Use the model to predict classes for the dataset
predicted_classes = [predict_image(image.reshape(1, IMG_SIZE, IMG_SIZE), W_conv, b_conv, W_fc, b_fc) for image in train_data]

# Extract actual classes from the labels
actual_classes = [np.argmax(label) for label in train_labels]

# Calculate accuracy
correct_predictions = np.sum(np.array(predicted_classes) == np.array(actual_classes))
accuracy = correct_predictions / len(train_data)

print(f"Model Accuracy: {accuracy * 100:.2f}%")


# In[33]:


def predict_image(image, W_conv, b_conv, W_fc, b_fc):
 
    # Forward pass
    conv_output = convolution(image, W_conv, b_conv)
    relu_output = relu(conv_output)
    pooled_output = max_pooling(relu_output, pool_size, stride=pool_size)
    flattened_output = pooled_output.flatten().reshape(-1, 1)
    fc_output = fully_connected(flattened_output, W_fc, b_fc)
    
    # Return the index of the maximum value as the predicted class
    return np.argmax(fc_output)

# Use the model to predict classes for the dataset
predicted_classes = [predict_image(image.reshape(1, IMG_SIZE, IMG_SIZE), W_conv, b_conv, W_fc, b_fc) for image in train_data]

# Extract actual classes from the labels
actual_classes = [np.argmax(label) for label in train_labels]

# Calculate accuracy
correct_predictions = np.sum(np.array(predicted_classes) == np.array(actual_classes))
accuracy = correct_predictions / len(train_data)

print(f"Model Accuracy: {accuracy * 100:.2f}%")


# In[38]:


def calculate_metrics(predicted_classes, actual_classes):
    
    true_positive = sum((np.array(predicted_classes) == 1) & (np.array(actual_classes) == 1))
    true_negative = sum((np.array(predicted_classes) == 0) & (np.array(actual_classes) == 0))
    false_positive = sum((np.array(predicted_classes) == 1) & (np.array(actual_classes) == 0))
    false_negative = sum((np.array(predicted_classes) == 0) & (np.array(actual_classes) == 1))

    precision = true_positive / (true_positive + false_positive)
    recall = true_positive / (true_positive + false_negative)
    f1 = 2 * (precision * recall) / (precision + recall)
    
    return precision, recall, f1, true_positive, true_negative, false_positive, false_negative

precision, recall, f1, true_positive, true_negative, false_positive, false_negative = calculate_metrics(predicted_classes, actual_classes)

evaluation_string = f"""
Model Evaluation:
Accuracy: {accuracy:.4f}
Precision: {precision:.4f}
Recall: {recall:.4f}
F1 Score: {f1:.4f}

"""

print(evaluation_string)



# In[24]:


import pickle

# Define the model parameters in a dictionary
model_params = {
    'W_conv': W_conv,
    'b_conv': b_conv,
    'W_fc': W_fc,
    'b_fc': b_fc
}

with open('IHB_Plantdispest.pkl', 'wb') as file:
    pickle.dump(model_params, file)

print("Model saved successfully!")


# In[25]:


import json
import pickle

# Load the model parameters from pickle file
with open('IHB_Plantdispest.pkl', 'rb') as file:
    model_params = pickle.load(file)

# Convert the numpy arrays to lists for JSON serialization
for key, value in model_params.items():
    model_params[key] = value.tolist()

# Save the model parameters as JSON
with open('IHB_Plantdispest.json', 'w') as file:
    json.dump(model_params, file)


# In[ ]:


import os
import cv2
import pandas as pd

DATASET_PATH = r"C:\Users\ASUS\Desktop\Intelli\Plant Diseases and Pests"
CATEGORIES = os.listdir(DATASET_PATH)

IMG_SIZE = 64

all_data = []

for category in CATEGORIES:
    path = os.path.join(DATASET_PATH, category)
    for img in os.listdir(path):
        try:
            img_array = cv2.imread(os.path.join(path, img), cv2.IMREAD_GRAYSCALE)
            resized_array = cv2.resize(img_array, (IMG_SIZE, IMG_SIZE))
            
            flattened = resized_array.flatten() / 255.0 
            
            row = list(flattened) + [category]
            
            all_data.append(row)
        except Exception as e:
            print(f"Error processing image {img} from category {category}: {e}")

columns = [f"pixel{i}" for i in range(1, IMG_SIZE * IMG_SIZE + 1)] + ['label']

df = pd.DataFrame(all_data, columns=columns)

df_shuffled = df.sample(frac=1).reset_index(drop=True)

df_shuffled.to_csv('Plant_Diseases_Pests_shuf.csv', index=False)

