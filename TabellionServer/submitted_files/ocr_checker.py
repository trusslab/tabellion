import cv2
import sys
import pytesseract


def main():
    # if there is no enough arguments passed in, give the error and quit
    if len(sys.argv) < 2:
        print('Usage: python ocr_checker.py image.jpg')
        sys.exit(1)

    pathOfImg = sys.argv[1]
    tesConfig = "-l eng --oem 1 --psm 3"

    #print("Going to read image with cv2")
    im = cv2.imread(pathOfImg, cv2.IMREAD_COLOR)
    #print("Image read, going to do ocr on the image")

    textOfImg = pytesseract.image_to_string(im, config=tesConfig)
    #print("ocr finished, going to print")

    print(textOfImg)


if __name__ == "__main__":
    main()


