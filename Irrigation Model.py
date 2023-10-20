#!/usr/bin/env python
# coding: utf-8

# In[1]:


import pandas as pd
import random


# In[2]:


train_df = pd.read_csv(r"C:\Users\ASUS\Desktop\Intelli\train_data.csv")
test_df = pd.read_csv(r"C:\Users\ASUS\Desktop\Intelli\test_data.csv")


# In[3]:


train_df.head()


# In[4]:


missing_values = train_df.isnull().sum()

stats = train_df.describe()

missing_values, stats


# In[5]:


import matplotlib.pyplot as plt

fig, axes = plt.subplots(3, 1, figsize=(10, 12))

axes[0].hist(train_df['humidity'], bins=20, color='blue', edgecolor='black')
axes[0].set_title('Distribution of Humidity')
axes[0].set_xlabel('Humidity (%)')
axes[0].set_ylabel('Frequency')

axes[1].hist(train_df['raindrop'], bins=20, color='green', edgecolor='black')
axes[1].set_title('Distribution of Raindrop Values')
axes[1].set_xlabel('Raindrop Value')
axes[1].set_ylabel('Frequency')

axes[2].hist(train_df['temperature'], bins=20, color='red', edgecolor='black')
axes[2].set_title('Distribution of Temperature')
axes[2].set_xlabel('Temperature (°C)')
axes[2].set_ylabel('Frequency')

plt.tight_layout()
plt.show()


# In[ ]:





# In[6]:


def bootstrap_sample(data, size=None):
    if size is None:
        size = len(data)
    indices = [random.randint(0, len(data) - 1) for _ in range(size)]
    return data.iloc[indices]


# In[7]:


def gini_impurity(labels):
    if labels.empty:
        return 0
    prob_positive = sum(labels) / len(labels)
    return 2 * prob_positive * (1 - prob_positive)


# In[8]:


def find_best_split(data, target_column):
    best_gini = float('inf')
    best_feature = None
    best_threshold = None
    features = data.columns.drop(target_column)
    for feature in features:
        sorted_data = data.sort_values(by=feature)
        unique_values = sorted_data[feature].unique()
        for i in range(1, len(unique_values)):
            threshold = (unique_values[i - 1] + unique_values[i]) / 2
            left_split = sorted_data[sorted_data[feature] <= threshold][target_column]
            right_split = sorted_data[sorted_data[feature] > threshold][target_column]
            left_weight = len(left_split) / len(sorted_data)
            right_weight = len(right_split) / len(sorted_data)
            gini = left_weight * gini_impurity(left_split) + right_weight * gini_impurity(right_split)
            if gini < best_gini:
                best_gini = gini
                best_feature = feature
                best_threshold = threshold
    return best_feature, best_threshold


# In[9]:


class Node:
    pass

class DecisionNode(Node):
    def __init__(self, feature, threshold, left, right):
        self.feature = feature
        self.threshold = threshold
        self.left = left
        self.right = right

class LeafNode(Node):
    def __init__(self, label):
        self.label = label


# In[10]:


def build_tree(data, target_column, max_depth=3):
    unique_labels = data[target_column].unique()
    if len(unique_labels) == 1 or max_depth == 0:
        return LeafNode(unique_labels[0])
    feature, threshold = find_best_split(data, target_column)
    if feature is None:
        return LeafNode(data[target_column].mode()[0])
    left_data = data[data[feature] <= threshold]
    right_data = data[data[feature] > threshold]
    left_tree = build_tree(left_data, target_column, max_depth - 1)
    right_tree = build_tree(right_data, target_column, max_depth - 1)
    return DecisionNode(feature, threshold, left_tree, right_tree)


# In[11]:


def predict(tree, data_point):
    if isinstance(tree, LeafNode):
        return tree.label
    if data_point[tree.feature] <= tree.threshold:
        return predict(tree.left, data_point)
    return predict(tree.right, data_point)


# In[12]:


def build_rf(data, target_column, n_trees=5, max_depth=3):
    return [build_tree(bootstrap_sample(data), target_column, max_depth) for _ in range(n_trees)]


# In[13]:


def predict_rf(forest, data_point):
    predictions = [predict(tree, data_point) for tree in forest]
    return max(set(predictions), key=predictions.count)


# In[14]:


# Metrics
def calculate_precision(true_positive, false_positive):
    return true_positive / (true_positive + false_positive)

def calculate_recall(true_positive, false_negative):
    return true_positive / (true_positive + false_negative)

def calculate_f1(precision, recall):
    return 2 * (precision * recall) / (precision + recall)


# In[17]:


forest = build_rf(train_df, 'soilMoistureStatus', n_trees=50, max_depth=3)
predictions = test_df.apply(lambda row: predict_rf(forest, row), axis=1)

accuracy = (predictions == test_df['soilMoistureStatus']).mean()

true_positive = sum((predictions == 1) & (test_df['soilMoistureStatus'] == 1))
true_negative = sum((predictions == 0) & (test_df['soilMoistureStatus'] == 0))
false_positive = sum((predictions == 1) & (test_df['soilMoistureStatus'] == 0))
false_negative = sum((predictions == 0) & (test_df['soilMoistureStatus'] == 1))

precision = calculate_precision(true_positive, false_positive)
recall = calculate_recall(true_positive, false_negative)
f1 = calculate_f1(precision, recall)

# Print the metrics
evaluation_string = f"""
Model Evaluation:
=====================================
Accuracy: {accuracy:.4f}
Precision: {precision:.4f}
Recall: {recall:.4f}
F1 Score: {f1:.4f}


"""

print(evaluation_string)


# In[16]:


import pickle

# Saving the model
with open('random_forest_model.pkl', 'wb') as model_file:
    pickle.dump(forest, model_file)

# To load the model later:
# with open('random_forest_model.pkl', 'rb') as model_file:
#     loaded_forest = pickle.load(model_file)


# In[ ]:




