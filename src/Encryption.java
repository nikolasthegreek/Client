import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

//====
//same as Encription.java from client
//====

public class Encryption {
    private static PrivateKey PrKey;// private key for RSA encription
    private static PublicKey PubKey;// public key for RSA encription(to be sent to others)
    private static Cipher DecryptCipher;

    private PublicKey ClientPubKey;// public key from clients not the servers
    private Cipher EncryptCipher;

    private static boolean KeysReady = false;
    
    public static void GenerateKeys(){
        if(KeysReady){return;}//ensures Keys wond be generated again and they will not be changed during a setion
        try{
            System.out.println("~Generating keys");
            //sets up generator
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            //generates keys
            KeyPair pair = generator.generateKeyPair();
            //stores keys localy
            PrKey=pair.getPrivate();
            PubKey=pair.getPublic();
            System.out.println("~Keys ready");
        }catch(Exception e){
            System.out.println("&failed to generate keys : "+e);
        }        
    }
    
    public static void InitDecrypCipher(){
        //initiate the decryption cipher since the private key will be the same with all client interactions
        try{
            DecryptCipher = Cipher.getInstance("RSA");
            DecryptCipher.init(Cipher.DECRYPT_MODE, PrKey);
            System.out.println("~Cipher ready");
        }catch(Exception e){
            System.out.println("&failed to initialise decription cipher : "+e);
        }
    }
    
    public static String DeCrypt(String MessageSTR) {
        try{
            byte[] MessageBEncrypted = Bytehex.decodeHexString(MessageSTR);//turn string to bytes
            byte[] MessageBDecrypted = DecryptCipher.doFinal(MessageBEncrypted);//decypher
            String Message = new String(MessageBDecrypted,StandardCharsets.US_ASCII);//turn back to a string
            return Message;
        }catch(Exception e){
            System.out.println("&failed decrypt : "+e);
            return null;
        }
    }
    
    public static String GetPublicKey(){
        String StringKey =new String (Bytehex.encodeHexString(PubKey.getEncoded()));
        return StringKey;
    }
    
    public boolean InitEncryptCipher(String ClientKeyHex){
        try{
            byte[] EncodedKey = Bytehex.decodeHexString(ClientKeyHex);
            KeyFactory factory =KeyFactory.getInstance("RSA");
            PublicKey ClientPubKey = factory.generatePublic(new X509EncodedKeySpec(EncodedKey));
            //initiate the encryption cipher for instance of class (thread of client interaction)
            EncryptCipher = Cipher.getInstance("RSA");
            EncryptCipher.init(Cipher.ENCRYPT_MODE, ClientPubKey);
            return true;
        }catch(InvalidKeyException e){
            System.out.println("&Bad public key : "+e);
            return false;
        }catch(Exception e){
            System.out.println("&failed to init encrypt: "+e);
            return false;
        }
        
    }
    
    public String Encript(String Message){
        try{
            byte[] MessageBUnencrypted = Message.getBytes(StandardCharsets.US_ASCII);//Binarises the message
            byte[] MessageBEncrypted = EncryptCipher.doFinal(MessageBUnencrypted);// encripts it
            String EncryptedMessage = Bytehex.encodeHexString(MessageBEncrypted);// turns it back to a HEX string
            return EncryptedMessage;
        }catch(Exception e){
            System.out.println("&failed to encrypt : "+e);
            return null;
        }
        
    }

    static public String Hash(String HexSalt,String string){
        try{
            SecureRandom random = new SecureRandom();
            byte[] salt = Bytehex.decodeHexString(HexSalt);
            KeySpec spec = new PBEKeySpec(string.toCharArray(), salt, 65536, 640);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return Bytehex.encodeHexString(hash);
        }catch(Exception e){
            System.err.println("&failed to hash : "+e);
        }
        return null;
    }

    static public String GenerateSaltHex(){
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Bytehex.encodeHexString(salt);
        
    }
}