import piexif
from PIL import Image
import sys
import cv2
import pytesseract
import argparse
import os
import io

filename=sys.argv[1]

def detect_text(path):
    """Detects text in the file."""
    from google.cloud import vision
    client = vision.ImageAnnotatorClient()

    with io.open(path, 'rb') as image_file:
        content = image_file.read()

    image = vision.types.Image(content=content)

    response = client.text_detection(image=image)
    texts = response.text_annotations
    text_we_get = ""
    for text in texts:
        text_we_get = '{}'.format(text.description)
        print(text_we_get[:-1], end='')
        break
    return text_we_get
    # print('Texts:')
    '''
    for text in texts:
        print('\n"{}"'.format(text.description))

        vertices = (['({},{})'.format(vertex.x, vertex.y)
                    for vertex in text.bounding_poly.vertices])

        print('bounds: {}'.format(','.join(vertices)))
    '''

text = detect_text(filename)

zeroth_ifd = {
              piexif.ImageIFD.ImageDescription: text.encode(encoding='UTF-8',errors='strict')
              }

exif_dict = {"0th":zeroth_ifd}
exif_bytes = piexif.dump(exif_dict)
im = Image.open(filename)
im.save(filename, exif=exif_bytes)

'''
config = ('-l eng --oem 1 --psm 3')

# construct the argument parse and parse the arguments
ap = argparse.ArgumentParser()
ap.add_argument("-i", "--image", required=True,
	help="path to input image to be OCR'd")
ap.add_argument("-p", "--preprocess", type=str, default="thresh",
	help="type of preprocessing to be done")
args = vars(ap.parse_args())

# load the example image and convert it to grayscale
image = cv2.imread(args["image"])
gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
 
# check to see if we should apply thresholding to preprocess the
# image
if args["preprocess"] == "thresh":
	gray = cv2.threshold(gray, 0, 255,
		cv2.THRESH_BINARY | cv2.THRESH_OTSU)[1]
 
# make a check to see if median blurring should be done to remove
# noise
elif args["preprocess"] == "blur":
	gray = cv2.medianBlur(gray, 3)
 
# write the grayscale image to disk as a temporary file so we can
# apply OCR to it
cv2.imwrite(filename, gray)

text = pytesseract.image_to_string(Image.open(filename), config=config)

print(text, end='')

zeroth_ifd = {
              piexif.ImageIFD.ImageDescription: text.encode(encoding='UTF-8',errors='strict')
              }

exif_dict = {"0th":zeroth_ifd}
exif_bytes = piexif.dump(exif_dict)
im = Image.open(filename)
im.save(filename, exif=exif_bytes)

img = Image.open(filename)
#print(img.info)
'''
'''
exif_dict = piexif.load(img.info['exif'])

altitude = exif_dict['GPS'][piexif.GPSIFD.GPSAltitude]
print(altitude)
'''