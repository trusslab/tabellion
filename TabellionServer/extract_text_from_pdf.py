# import PyPDF2 
# import textract
# from nltk.tokenize import word_tokenize
# from nltk.corpus import stopwords
import sys

# https://medium.com/@rqaiserr/how-to-convert-pdfs-into-searchable-key-words-with-python-85aab86c544f
# https://stackoverflow.com/questions/34837707/how-to-extract-text-from-a-pdf-file

import fitz
firstDoc = fitz.open(sys.argv[1])

pageCount = firstDoc.pageCount

detailText = ""

for pageNum in range(pageCount):
    tempText = firstDoc.loadPage(pageNum).getText()
    for char in tempText:
        if char == '\t':
            detailText += " "
        else: 
            detailText += char
    detailText += "\n\n"

indexOfFirstNewLine = detailText.find('\n')

title = detailText[0 : indexOfFirstNewLine]

detailText = detailText[indexOfFirstNewLine :]

print(title, end="")

print("[detailText]:" + detailText, end="")
        

