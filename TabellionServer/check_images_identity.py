# Created by Myles
# Created at 04/09/2019
# Last modified by Myles
# Last modified at 04/11/2019

# Credit is given to https://www.pyimagesearch.com/2014/09/15/python-compare-two-images/

# import the necessary packages
from skimage import measure
import matplotlib.pyplot as plt
import numpy as np
import cv2
import sys
import time
import glob, os
import threading 

def mse(imageA, imageB):
	# the 'Mean Squared Error' between the two images is the
	# sum of the squared difference between the two images;
	# NOTE: the two images must have the same dimension
	err = np.sum((imageA.astype("float") - imageB.astype("float")) ** 2)
	err /= float(imageA.shape[0] * imageA.shape[1])
	
	# return the MSE, the lower the error, the more "similar"
	# the two images are
	return err
 
def compare_images(imageA, imageB, title):
	# compute the mean squared error and structural similarity
	# index for the images
	# m = mse(imageA, imageB)
	s = measure.compare_ssim(imageA, imageB)
	return s

original_directory = os.getcwd()
os.chdir(sys.argv[1])

originalImagesNames = []

for file in glob.glob("Ndoc*.png"):
	originalImagesNames.append(file)

os.chdir(original_directory)
os.chdir(sys.argv[2])

screenshotImagesNames = []

for file in glob.glob("screenshot*"):
	screenshotImagesNames.append(file)

total_images = len(screenshotImagesNames)
thread_pool = []
the_real_result = True

originalImagesNames.sort()
screenshotImagesNames.sort()

os.chdir(original_directory)

def challenge_real_result(index):
	# load the images -- the original, the original + screenshot
	original = cv2.imread(sys.argv[1] + "/" + originalImagesNames[index])
	screenshot = cv2.imread(sys.argv[2] + "/" + screenshotImagesNames[index])
	
	# convert the images to grayscale
	original = cv2.cvtColor(original, cv2.COLOR_BGR2GRAY)
	screenshot = cv2.cvtColor(screenshot, cv2.COLOR_BGR2GRAY)
	
	# compare the images
	result = compare_images(original, screenshot, "Original vs. Screenshot")

	# print("Comparing " + originalImagesNames[index] + " with " + screenshotImagesNames[index] + ", we get:", result)
	
	if result <= 0.99:
		global the_real_result
		the_real_result = False

for i in range(total_images):
	thread_pool.append(threading.Thread(target=challenge_real_result, args=(i,)))

for thread in thread_pool:
	thread.start()

for thread in thread_pool:
	thread.join()

print(the_real_result, end='')

'''

# load the images -- the original, the original + screenshot
original = cv2.imread(sys.argv[1])
screenshot = cv2.imread(sys.argv[2])
 
# convert the images to grayscale
original = cv2.cvtColor(original, cv2.COLOR_BGR2GRAY)
screenshot = cv2.cvtColor(screenshot, cv2.COLOR_BGR2GRAY)
 
# compare the images
compare_images(original, screenshot, "Original vs. Screenshot")

'''
