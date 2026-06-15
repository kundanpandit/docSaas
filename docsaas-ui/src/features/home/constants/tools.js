import {
  Files,
  Scissors,
  Minimize2,
  FileText,
  FileType,
  Image,
  FileImage,
  ScanText,
  Search,
  ImageDown,
  ImageUp,
  RefreshCcw,
  Bot,
  MessageSquare,
} from "lucide-react";

export const PDF_TOOLS = [
  {
    id: "merge-pdf",
    title: "Merge PDF",
    description: "Combine multiple PDF files into one document.",
    icon: Files,
  },
  {
    id: "split-pdf",
    title: "Split PDF",
    description: "Split PDF pages into separate documents.",
    icon: Scissors,
  },
  {
    id: "compress-pdf",
    title: "Compress PDF",
    description: "Reduce PDF size without losing quality.",
    icon: Minimize2,
  },
  {
    id: "pdf-to-word",
    title: "PDF to Word",
    description: "Convert PDF files into editable Word documents.",
    icon: FileText,
  },
  {
    id: "word-to-pdf",
    title: "Word to PDF",
    description: "Convert Word documents into PDF files.",
    icon: FileType,
  },
  {
    id: "pdf-to-images",
    title: "PDF to Images",
    description: "Convert PDF pages into image files.",
    icon: Image,
  },
  {
    id: "images-to-pdf",
    title: "Images to PDF",
    description: "Convert images into a single PDF document.",
    icon: FileImage,
  },
];

export const OCR_TOOLS = [
  {
    id: "image-to-text",
    title: "Image to Text",
    description: "Extract text from images using OCR.",
    icon: ScanText,
  },
  {
    id: "scanned-pdf-ocr",
    title: "Scanned PDF OCR",
    description: "Convert scanned PDFs into searchable PDFs.",
    icon: Search,
  },
];

export const IMAGE_TOOLS = [
  {
    id: "compress-image",
    title: "Compress Image",
    description: "Reduce image file size.",
    icon: ImageDown,
  },
  {
    id: "jpg-to-png",
    title: "JPG to PNG",
    description: "Convert JPG images into PNG format.",
    icon: Image,
  },
  {
    id: "png-to-jpg",
    title: "PNG to JPG",
    description: "Convert PNG images into JPG format.",
    icon: ImageUp,
  },
  {
    id: "webp-converter",
    title: "WEBP Converter",
    description: "Convert images to and from WEBP.",
    icon: RefreshCcw,
  },
];

export const AI_TOOLS = [
  {
    id: "ai-summary",
    title: "AI PDF Summary",
    description: "Generate AI-powered summaries from PDFs.",
    icon: Bot,
  },
  {
    id: "chat-with-pdf",
    title: "Chat With PDF",
    description: "Ask questions and get answers from PDF files.",
    icon: MessageSquare,
  },
];