package org.jkiss.dbeaver.model.impl.data.transformers;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeTransformer;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.data.ProxyValueHandler;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentBytes;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AutoDecompressAttributeTransformer implements DBDAttributeTransformer {
    public void transformAttribute(
            DBCSession session,
            DBDAttributeBinding attribute,
            List<Object[]> rows,
            Map<String, Object> options
    ) throws DBException {

        attribute.setPresentationAttribute(
                new TransformerPresentationAttribute(attribute, "COMPRESSED", -1, DBPDataKind.BINARY));

        String algorithm = options.getOrDefault("algorithm", "auto").toString().trim().toLowerCase();
        int maxSize = Integer.parseInt(options.getOrDefault("max_size", "10485760").toString().trim());
        attribute.setTransformHandler(new AutoDecompressAttributeTransformer.AutoDecompressHandler(
                attribute.getValueHandler(),
                algorithm,
                maxSize
        ));


    }

    private static class AutoDecompressHandler extends ProxyValueHandler {
        private static final int MAX_SIZE = 10*1024*1024;
        private final String algorithm;
        private final int maxSize;

        public AutoDecompressHandler(DBDValueHandler target, String algorithm, int maxSize) {
            super(target);
            this.algorithm = algorithm;
            this.maxSize = maxSize;
        }

        @Override
        public String getValueDisplayString(DBSTypedObject column, Object value, DBDDisplayFormat format) {
            byte[] bytes = null;
            if (value instanceof byte[]) {
                bytes = (byte[]) value;
            } else if (value instanceof JDBCContentBytes) {
                bytes = ((JDBCContentBytes) value).getRawValue();
            }
            try(InputStream rawStream = new ByteArrayInputStream(bytes)) {
                try (CompressorInputStream cin = makeStream(rawStream)) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buf = new byte[32];
                    int read = 0;
                    do {
                        out.write(buf, 0, read);
                        read = cin.read(buf);
                    } while(read > -1 && out.size() < this.maxSize);
                    return new String(out.toByteArray(), StandardCharsets.UTF_8);
                }
            } catch (Exception e) {
                return "UNABLE TO DECOMPRESS: "+e.getMessage()+":::"+new String(bytes);
            }
        }

        private CompressorInputStream makeStream(InputStream plain) throws CompressorException {
            CompressorStreamFactory fac = new CompressorStreamFactory();

            if(fac.getCompressorInputStreamProviders().containsKey(this.algorithm)) {
                return fac.createCompressorInputStream(this.algorithm, plain, true);
            } else {
                return fac.createCompressorInputStream(CompressorStreamFactory.detect(plain), plain, true);
            }
        }
    }
}
