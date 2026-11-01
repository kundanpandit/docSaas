package com.docsaas.api.processing;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.docsaas.api.enums.ToolCode;

/** Simple single-image format conversions. */
@Configuration
public class ImageFormatProcessors {

    @Bean
    ToolProcessor jpgToPngProcessor() {
        return new ToolProcessor() {
            @Override
            public ToolCode tool() {
                return ToolCode.JPG_TO_PNG;
            }

            @Override
            public ProcessingResult process(ProcessingContext ctx) throws IOException {
                ProcessingContext.InputFile input = ctx.inputs().get(0);
                BufferedImage image = ImageSupport.read(input);
                Path out = ctx.workDir().resolve("converted.png");
                ImageSupport.writePng(image, out);
                return new ProcessingResult(out, FileNames.baseName(input.originalName()) + ".png",
                        "image/png", "Converted to PNG.");
            }
        };
    }

    @Bean
    ToolProcessor pngToJpgProcessor() {
        return new ToolProcessor() {
            @Override
            public ToolCode tool() {
                return ToolCode.PNG_TO_JPG;
            }

            @Override
            public ProcessingResult process(ProcessingContext ctx) throws IOException {
                ProcessingContext.InputFile input = ctx.inputs().get(0);
                BufferedImage image = ImageSupport.read(input);
                Path out = ctx.workDir().resolve("converted.jpg");
                // Transparent areas become white, since JPEG has no alpha channel.
                ImageSupport.writeJpeg(image, 0.92f, out);
                return new ProcessingResult(out, FileNames.baseName(input.originalName()) + ".jpg",
                        "image/jpeg", "Converted to JPG. Transparent areas were filled with white.");
            }
        };
    }
}
