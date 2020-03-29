import PyPDF2 
import textract
from nltk.tokenize import word_tokenize
from nltk.corpus import stopwords
import sys
from tika import parser

# https://medium.com/@rqaiserr/how-to-convert-pdfs-into-searchable-key-words-with-python-85aab86c544f
# https://stackoverflow.com/questions/34837707/how-to-extract-text-from-a-pdf-file

'''
firstPdf = open(sys.argv[1], "rb")

firstPdfReader = PyPDF2.PdfFileReader(firstPdf)

numOfPagesFirstPdf = firstPdfReader.numPages
count = 0
firstPdfContent = ""

while count < numOfPagesFirstPdf:
    pageObj = firstPdfReader.getPage(count)
    count += 1
    firstPdfContent += pageObj.extractText()

if firstPdfContent != "":
    firstPdfContent = firstPdfContent
else:
    print("OCR is being used...")
    firstPdfContent = textract.process(sys.argv[1], method='tesseract', language='eng')

print(firstPdfContent)

firstPdf.close()
'''
'''
firstRaw = parser.from_file(sys.argv[1])
firstContent = firstRaw['content']
firstContent = firstContent.replace("\n", "")
firstContent = firstContent.replace(" ", "")
firstContent = firstContent.replace("\t", "")

print(firstContent)
'''

import fitz
firstDoc = fitz.open(sys.argv[1])
secondDoc = fitz.open(sys.argv[2])

pageCount = 0

if firstDoc.pageCount >= secondDoc.pageCount:
    pageCount = secondDoc.pageCount
else:
    pageCount = firstDoc.pageCount


def process_pdf_content(content):
    content = content.replace("\n", "")
    content = content.replace(" ", "")
    content = content.replace("\t", "")
    return content

signedPages = ""
isOver = 0

for pageNum in range(pageCount):
    if pageNum >= firstDoc.pageCount or \
        pageNum >= secondDoc.pageCount or \
        process_pdf_content(firstDoc.loadPage(pageNum).getText()) != process_pdf_content(secondDoc.loadPage(pageNum).getText()):
        if len(signedPages) > 0:
            signedPages = signedPages[:-1]
            isOver = 1
            print(signedPages, end="")
            break
    signedPages += str(pageNum + 1) + "%"

if len(signedPages) > 0 and isOver == 0:
    signedPages = signedPages[:-1]

print(signedPages, end="")
        

