import java.nio.file.*;
import com.android.ide.common.vectordrawable.Svg2Vector;

/** Uses the Android build tools' SVG importer; never substitutes logo lettering. */
class ConvertSvg {
    public static void main(String[] args) throws Exception {
        try (var out = Files.newOutputStream(Path.of(args[1]))) {
            var diagnostics = Svg2Vector.parseSvgToXml(Path.of(args[0]), out);
            if (!diagnostics.isBlank()) throw new IllegalStateException(diagnostics);
        }
    }
}
