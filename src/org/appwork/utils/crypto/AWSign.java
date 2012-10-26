package org.appwork.utils.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.appwork.utils.encoding.Base64;

public class AWSign {
    private static SecureRandom sr;
    static {
        try {
            AWSign.sr = new SecureRandom();
        } catch (final Throwable e) {
            e.printStackTrace();
        }
    }

    public static void createKeyPair() throws NoSuchAlgorithmException {
        final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        final KeyPair keyPair = keyPairGenerator.genKeyPair();
        System.out.println("PUBLIC  " + Base64.encodeToString(keyPair.getPublic().getEncoded(), false));
        System.out.println("PRIVATE " + Base64.encodeToString(keyPair.getPrivate().getEncoded(), false));
    }

    /**
     * @param bytes
     * @param pk
     * @param salt
     * @return
     * @throws SignatureViolationException
     */
    public static byte[] createSign(final byte[] bytes, final PrivateKey pk, final boolean salt) throws SignatureViolationException {
        try {

            final Signature sig = Signature.getInstance("Sha256WithRSA");

            sig.initSign(pk);
            final byte[] saltBytes = AWSign.getSalt(salt);

            if (saltBytes != null) {
                sig.update(saltBytes);
            }

            sig.update(bytes, 0, bytes.length);

            final byte[] ret = sig.sign();
            if (!salt) { return ret; }

            final byte[] merged = new byte[ret.length + saltBytes.length];
            System.arraycopy(saltBytes, 0, merged, 0, saltBytes.length);
            System.arraycopy(ret, 0, merged, saltBytes.length, ret.length);
            return merged;

        } catch (final Throwable e) {
            throw new SignatureViolationException(e);
        }
    }

    public static byte[] createSign(final File f, final PrivateKey publicKey, final boolean salt, final byte[] addInfo) throws SignatureViolationException {
        try {

            final Signature sig = Signature.getInstance("Sha256WithRSA");

            sig.initSign(publicKey);

            InputStream input = null;
            try {

                final byte[] saltBytes = AWSign.getSalt(salt);

                if (saltBytes != null) {
                    sig.update(saltBytes);
                }
                if (addInfo != null) {
                    sig.update(addInfo, 0, addInfo.length);
                }
                final byte[] buffer = new byte[1024];
                int len;
                input = new FileInputStream(f);
                while ((len = input.read(buffer)) != -1) {
                    if (len > 0) {
                        sig.update(buffer, 0, len);
                    }
                }
                final byte[] ret = sig.sign();
                if (!salt) { return ret; }

                final byte[] merged = new byte[ret.length + saltBytes.length];
                System.arraycopy(saltBytes, 0, merged, 0, saltBytes.length);
                System.arraycopy(ret, 0, merged, saltBytes.length, ret.length);
                return merged;

            } finally {

                try {
                    input.close();
                } catch (final Exception e) {
                }

            }

        } catch (final Throwable e) {
            throw new SignatureViolationException(e);
        }
    }

    public static void decryptRSA_AES(final File srcFile, final File dstFile, final PublicKey pk) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        boolean deleteDst = true;
        try {
            fis = new FileInputStream(srcFile);
            fos = new FileOutputStream(dstFile);
            final byte[] wrappedKey = new byte[256];
            final byte[] wrappedIV = new byte[256];
            final byte[] readDigest = new byte[32];
            int done = 0;
            int read = 0;
            while (done < 256 && (read = fis.read()) != -1) {
                wrappedKey[done++] = (byte) read;
            }
            done = 0;
            while (done < 256 && (read = fis.read()) != -1) {
                wrappedIV[done++] = (byte) read;
            }
            done = 0;
            while (done < 32 && (read = fis.read()) != -1) {
                readDigest[done++] = (byte) read;
            }
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, pk);
            final SecretKey key = new SecretKeySpec(cipher.doFinal(wrappedKey), "AES");
            cipher.init(Cipher.DECRYPT_MODE, pk);
            final byte[] iv = cipher.doFinal(wrappedIV);
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            final CipherInputStream cis = new CipherInputStream(fis, cipher);
            final byte[] buffer = new byte[32767];
            final int maxReadSize = buffer.length;
            while ((read = cis.read(buffer, 0, maxReadSize)) != -1) {
                if (read > 0) {
                    fos.write(buffer, 0, read);
                    md.update(buffer, 0, read);
                }
            }
            final byte[] generatedDigest = md.digest();
            if (Arrays.equals(generatedDigest, readDigest) == false) { throw new IOException("Hash failed!"); }
            fos.close();
            deleteDst = false;
        } finally {
            try {
                fis.close();
            } catch (final Throwable e) {
            }
            try {
                fos.close();
            } catch (final Throwable e) {
            }
            if (deleteDst) {
                dstFile.delete();
            }
        }
    }

    public static void encryptRSA_AES(final File srcFile, final File dstFile, final PrivateKey pk) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IOException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        boolean deleteDst = true;
        try {
            fis = new FileInputStream(srcFile);
            fos = new FileOutputStream(dstFile);
            final KeyGenerator keygen = KeyGenerator.getInstance("AES");
            if (AWSign.sr != null) {
                keygen.init(AWSign.sr);
            }
            byte[] iv = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
            if (AWSign.sr != null) {
                iv = AWSign.sr.generateSeed(16);
            }
            keygen.init(128);
            final SecretKey key = keygen.generateKey();
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, pk);
            fos.write(cipher.doFinal(key.getEncoded()));
            cipher.init(Cipher.ENCRYPT_MODE, pk);
            fos.write(cipher.doFinal(iv));
            fos.write(new byte[32]);
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
            final CipherOutputStream cos = new CipherOutputStream(new FilterOutputStream(fos) {

                @Override
                public void close() throws IOException {
                }

                @Override
                public void flush() throws IOException {
                    this.out.flush();
                }

                @Override
                public void write(final byte[] b) throws IOException {
                    this.out.write(b);
                }

                @Override
                public void write(final byte[] b, final int off, final int len) throws IOException {
                    this.out.write(b, off, len);
                }

                @Override
                public void write(final int b) throws IOException {
                    this.out.write(b);
                }

            }, cipher);
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            int read = 0;
            final byte[] buffer = new byte[32767];
            while ((read = fis.read(buffer)) != -1) {
                if (read > 0) {
                    cos.write(buffer, 0, read);
                    md.update(buffer, 0, read);
                }
            }
            cos.close();
            fos.getChannel().position(2 * 256);
            fos.write(md.digest());
            deleteDst = false;
        } finally {
            try {
                fis.close();
            } catch (final Throwable e) {
            }
            try {
                fos.close();
            } catch (final Throwable e) {
            }
            if (deleteDst) {
                dstFile.delete();
            }
        }
    }

    public static PublicKey getPublicKey(final String base64Encoded) throws SignatureViolationException {
        try {
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.decode(base64Encoded)));
        } catch (final InvalidKeySpecException e) {
            throw new SignatureViolationException(e);
        } catch (final NoSuchAlgorithmException e) {
            throw new SignatureViolationException(e);
        }
    }

    /**
     * @param salt
     * @return
     * @throws NoSuchAlgorithmException
     */
    public static byte[] getSalt(final boolean salt) throws NoSuchAlgorithmException {
        if (!salt) { return null; }

        final byte[] saltBytes = new byte[16];
        if (AWSign.sr != null) {
            AWSign.sr.nextBytes(saltBytes);
        }
        return saltBytes;

    }

    public static void main(final String[] args) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IOException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, InvalidKeySpecException {
        // AWSign.createKeyPair();

        // PUBLIC
        try {
            final PublicKey pub = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.decode("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAg+EQ1wHD62QGjzJalAkl1WjExeS345ZkCMtuyvqP3NLVpUbfZjc/IeHVi9qKBUPtV8ca8QfOZo8ACNIBvUxEiVy4YFE7vqZfBNV0uEz/kHSXxDlFeiv0+BFMgcXow0NYBjGDT02/1ddmjEMtnAXnjqUwlVPorzOmJoeuNSLCyCcOe0pKuF1yDha9TkEsaUcJ8kho+09kQvhMl5mKnuTUc81nIHHVb4GClRmFp1kfB9BbqPc9sL5jg1BrmjHMCD84HZk4OehxJ8AeA+veVRH2Gn6gcslPcrgNw1zK6VcXzCqsuZCAejAyDHnX+jay1SaxmHDgk5jc+agee+M2+QPiwQIDAQAB")));
            final PrivateKey pk = KeyFactory
                    .getInstance("RSA")
                    .generatePrivate(
                            new PKCS8EncodedKeySpec(
                                    Base64.decode("MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCD4RDXAcPrZAaPMlqUCSXVaMTF5LfjlmQIy27K+o/c0tWlRt9mNz8h4dWL2ooFQ+1XxxrxB85mjwAI0gG9TESJXLhgUTu+pl8E1XS4TP+QdJfEOUV6K/T4EUyBxejDQ1gGMYNPTb/V12aMQy2cBeeOpTCVU+ivM6Ymh641IsLIJw57Skq4XXIOFr1OQSxpRwnySGj7T2RC+EyXmYqe5NRzzWcgcdVvgYKVGYWnWR8H0Fuo9z2wvmODUGuaMcwIPzgdmTg56HEnwB4D695VEfYafqByyU9yuA3DXMrpVxfMKqy5kIB6MDIMedf6NrLVJrGYcOCTmNz5qB574zb5A+LBAgMBAAECggEAWLi/lYZQgjoG16tumI0W8N3NE71toST6I5iI7vFme48zwD9P5/pe9LJz8eSSWjx6nkUK8QDpcMHfqg9usCVxLmA8gj/kS7ytzBi2r47NmCd4OsC05x5PbdxldiDpGQRjYbdJub56wqhpCw/ezUqDn8muR6ftsIC01NMO9hxuoiv1tE1GXZwBo36YSPb5NsB0Og5p0w8gwogXo0/TLIOJVy5ysZGACrXMaSN7DX/XP5hp4rXEfbY9vQdegVShejIKOIc9r5+0btRPjGP7YkMRWvTQQt43jWgI8cBIFUcZ4fYmwegzGnl1OONVzXjum10B2E3R2vmDEZqrLVB22I5Y4QKBgQDq0W0/g9H7hyd0NroZP8yP0/bOZO6ZYzUNMRQgZ/BO76yMUId6+wi0RREzqya+r/ur+kDXCs+liTlbJ+KyjRv29ls40eDW5OpCG9ccFguzg1CUpyIRu2obKC5i59x3I4KGiUplumKcSE8QILD09DslvoSf2pHBIQKZNEdVMROObQKBgQCPxoGORYshbsptqZ5batMTWAeb5xeBn6rxBNDWAzeD+qXazOsTWYgU4310nq/Vqyc7UU18VPoRTTflUyhJFoFxJRjTEHxa/hKjIOGPYayCK2EHrMXHoSxZsUvSbSH1Y84zFAbDcRPylXg1pGnn5CyDB5jijS6mQxnT94TRgX1hJQKBgFwzcUcgNmIiFn7OQlJJt8O9wcoW3Y0C5EDSxYlX5obIGyNZN2k1ipxmBjQYfvUe2p4TfEQzrYbdE9VUGvJq79EPuI/d8P/QEJ92mQchLOUGqaxE197IjQguxc/2JJ3vJoA3Bixde/zLc6fsfi8getz+Ksstok+H66JGYb/0ri4dAoGAFnZeAVtOHGAR0kZAzmmHJquHLM1S99Z5P4SQGA+SmdUMGn4PcAt53kGYdSLht9EwpOzT3UvtccyNog926MxSVtoD4d3ef9zYDpJxixQofoHGfAt7LvA4XJ79iJeySYNZUNOdJuXAxxKhIEhan3cfmS0Trrl+A03SeDJgltbTPt0CgYEA1uPP5gpL029gtx3shiQFblpVl3AhUE1dmDITJYrGqD+06Z+nPHu73kOnVdPKgy9wYIIxcyx/DrQfcT5e1+IZy9bZ5OOIUVi9qNsQ1RhvFzEwo8tiE/1LX7XUIC2gIjyY0Q+VXLk03UgjV7qAgOg4X/foetGZn2NHmc4NUUaCoNE=")));
            byte[] sign = null;
            sign = AWSign.createSign("Apfelbaum".getBytes(), pk, true);

            System.out.println(Base64.encodeToString(sign, false));
            AWSign.verify("Apfelbaum".getBytes(), pub, sign, true);
            System.out.println("OK");

            final File file = new File(AWSign.class.getResource(AWSign.class.getSimpleName() + ".class").toURI());
            System.out.println("Sign File: " + file);
            final byte[] add = new byte[] { 1, 2, 3 };
            // add=null;
            sign = AWSign.createSign(file, pk, true, add);

            System.out.println(Base64.encodeToString(sign, false));
            AWSign.verify(file, pub, sign, true, add);
            System.out.println("OK2");

        } catch (final Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * @param salted
     *            TODO
     * @param decode
     * @param decode2
     * @throws SignatureViolationException
     */
    public static void verify(final byte[] dataToVerify, final PublicKey pub, byte[] signature, final boolean salted) throws SignatureViolationException {
        try {
            final Signature sig = Signature.getInstance("Sha256WithRSA");
            sig.initVerify(pub);
            if (salted) {
                final byte[] salt = new byte[signature.length - 256];
                System.arraycopy(signature, 0, salt, 0, salt.length);

                final byte[] actualSignature = new byte[256];
                System.arraycopy(signature, signature.length - 256, actualSignature, 0, actualSignature.length);
                signature = actualSignature;
                sig.update(salt);

            }

            sig.update(dataToVerify);
            if (!sig.verify(signature)) { throw new SignatureViolationException("Signatur Check Failed"); }
        } catch (final SignatureViolationException e) {
            throw e;
        } catch (final Throwable e) {

            throw new SignatureViolationException(e);
        }
    }

    /**
     * @param f
     * @param pub
     * @param additionalBytes
     *            TODO
     * @param bs
     * @throws SignatureViolationException
     */
    public static void verify(final File f, final PublicKey pub, final byte[] signature, final boolean salted, final byte[] additionalBytes) throws SignatureViolationException {
        try {
            AWSign.verify(f.toURI().toURL().openStream(), pub, signature, salted, additionalBytes);
        } catch (final SignatureViolationException e) {
            throw e;
        } catch (final Throwable e) {

            throw new SignatureViolationException(e);
        }

    }

    /**
     * @param openStream
     * @param pub
     * @param signature
     * @param salted
     * @param additionalBytes
     */
    public static void verify(final InputStream input, final PublicKey pub, byte[] signature, final boolean salted, final byte[] additionalBytes) throws SignatureViolationException {
        try {

            final Signature sig = Signature.getInstance("Sha256WithRSA");
            sig.initVerify(pub);

            if (salted) {
                final byte[] salt = new byte[signature.length - 256];
                System.arraycopy(signature, 0, salt, 0, salt.length);

                final byte[] actualSignature = new byte[256];
                System.arraycopy(signature, signature.length - 256, actualSignature, 0, actualSignature.length);
                signature = actualSignature;
                sig.update(salt);
            }
            if (additionalBytes != null) {
                sig.update(additionalBytes);
            }
            final byte[] buffer = new byte[16384];
            int len;
            while ((len = input.read(buffer)) != -1) {
                if (len > 0) {
                    sig.update(buffer, 0, len);
                }
            }
            if (!sig.verify(signature)) { throw new SignatureViolationException("Signatur Check Failed"); }
        } catch (final SignatureViolationException e) {
            throw e;
        } catch (final Throwable e) {

            throw new SignatureViolationException(e);
        } finally {
            try {
                input.close();
            } catch (final Exception e) {
            }

        }

    }

}
