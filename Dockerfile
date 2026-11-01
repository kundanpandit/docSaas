# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# ---------- runtime ----------
# Free engines: LibreOffice (Word to PDF) and Tesseract (OCR, English + Hindi).
FROM eclipse-temurin:21-jre-noble
RUN apt-get update \
 && apt-get install -y --no-install-recommends \
      libreoffice-writer-nogui \
      tesseract-ocr tesseract-ocr-eng tesseract-ocr-hin libtesseract5 \
      fonts-dejavu fonts-noto-core fonts-liberation2 \
 && rm -rf /var/lib/apt/lists/*

RUN useradd --system --create-home docsaas
WORKDIR /app
COPY --from=build /src/target/docsaas-api-*.jar app.jar
RUN mkdir -p /data/storage && chown -R docsaas /data /app
USER docsaas

ENV STORAGE_DIR=/data/storage \
    TESSDATA_DIR=/usr/share/tesseract-ocr/5/tessdata \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080 8000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
