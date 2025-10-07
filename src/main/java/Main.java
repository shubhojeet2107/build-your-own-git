import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.security.MessageDigest;
import java.util.zip.Deflater;

public class Main {
  public static void main(String[] args) throws IOException {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.err.println("Logs from your program will appear here!");

    // Store the first command-line argument (e.g., "init") into a 'command' variable.
     final String command = args[0];

    // Now, let's check what the command was and act on it.
     switch (command) {
       // If the command is "init", run this block of code
       case "init" -> {

         // Create a File object representing the main '.git' directory
         final File root = new File(".git");

         // Create the 'objects' subdirectory inside the '.git' to store all the content data
         new File(root, "objects").mkdirs();
         // Create the 'refs' subdirectory inside the'.git' to store pointers like branches and tags
         new File(root, "refs").mkdirs();
         // Create the File object representing the 'HEAD' file, which points to the current branch
         final File head = new File(root, "HEAD");

         try {
           // Physically create the empty '.git/HEAD' file on the filesystem.
           head.createNewFile();
           // Write content to the HEAD file to make it point to the 'main' branch by default
           Files.write(head.toPath(), "ref: refs/heads/main\n".getBytes());
           // Print a success message
           System.out.println("Initialized git directory");
         } catch (IOException e) {
           throw new RuntimeException(e);
         }
       }

       case "cat-file" -> {
         // We only handle the "-p" flag for pretty-printing in this stage
         final String flag = args[1];
         final String hashCode = args[2];

         // 1) Find the object file path from the hashCode
         String directoryName = hashCode.substring(0, 2);
         String fileName = hashCode.substring(2);

         // to get the path go to .git -> objects -> directoryName -> filename
         Path objectPath = Paths.get(".git", "objects", directoryName, fileName);

         try {
           // 2) try to read the file. THIS is the line that can fail.
           // A byte is the most basic unit of data a computer can read. This is crucial because you are dealing with a compressed file, which is not plain text but raw binary data.
           byte[] compressedBytes = Files.readAllBytes(objectPath);

           // 3) Decompress the bytes using java built-in Inflater (ZLIB)
           //  Create a new Inflater object, which is Java's engine for decompressing ZLIB data
           Inflater inflater = new Inflater();
           // Give the inflater the compressed data it needs to work on.
           inflater.setInput(compressedBytes);
           // Create a flexible, in-memory "bucket" to hold the decompressed data as it's processed.
           ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
           // Create a small, temporary buffer (like a cup) to hold chunks of decompressed data.
           byte[] buffer = new byte[1024];
           // Loop as long as there is more compressed data to process
           while(!inflater.finished()){
             // Decompress a chunk of data, fill our buffer with it, and get the number of bytes written.
             int count = inflater.inflate(buffer);
             // Write the valid chunk of data from the buffer into our main output "bucket".
             outputStream.write(buffer, 0, count);
           }
           inflater.end();
           // Get the final, complete decompressed data from the output stream as a single byte array.
           byte[] decompressedBytes = outputStream.toByteArray();

           // 4) Find the null-byte separator between the header and content
           // Create a variable to store the position of the null-byte, initializing it to -1
           int nullByteIndex = -1;
           // Loop through every single byte of the decompressed data, one by one.
           for(int i=0; i<decompressedBytes.length; i++){
             // Check if the current byte is the null byte separator (represented by the value 0).
             if (decompressedBytes[i] == 0) {
               // If it is, save its position (index).
               nullByteIndex = i;
               // Stop the loop immediately, since we've found the first separator.
               break;
             }
           }

           // 5) Extract the content after the separator and print it
           // Create a new String using only the part of the byte array *after* the null-byte separator.
           String content = new String(decompressedBytes, nullByteIndex + 1, decompressedBytes.length - (nullByteIndex + 1));
           // Print the final content to the console without adding an extra newline at the end.
           System.out.print(content);

         } catch (java.nio.file.NoSuchFileException e) {
           // This block runs specifically when the file is not found (i.e., invalid HashCode)
           System.err.println("fatal: Not a valid object name " + hashCode);
           System.exit(1); // Exit with an error code

         } catch (IOException | DataFormatException e) {
           // catch other IO errors
             throw new RuntimeException(e);
         }
       }

       case "hash-object" -> {
         String flag = null;
         String filePathString;

         // case 1: Case with a flag: hash-object '-w' <file>
         if(args.length == 3){
           flag = args[1];
           filePathString = args[2];
           if(!"-w".equals(flag)) {
             System.err.println("fatal: unknown flag: " + flag);
             System.exit(1);
           }
         }
         // case 2: Case without a flag: hash-object <file>
         else if(args.length == 2){
           filePathString = args[1];
         }else{
           System.err.println("fatal: usage: hash-object [-w] <file>");
           System.exit(1);
           return;
         }

         // 1) Make sure the file is present
         Path userFilePath = Paths.get(filePathString);

         if(Files.notExists(userFilePath)){
           System.err.println("fatal: file does not exist: " + userFilePath);
           System.exit(1);
         }

         System.out.println("File found at: " +userFilePath);
         if(flag != null){
           System.out.println("Write flag (-w) is present.");
         }

         try{
           // 2) read the file
           // file's raw content into byte array.
           // This reads the pure binary data, avoiding any text encoding issues.
           byte[] fileBytes = Files.readAllBytes(userFilePath);

           // 3) create blob
           // get the size of the content in bytes.
           int fileLength = fileBytes.length;

           // Create the Git header string: "blob <size>\0".
           String header = "blob " +fileLength+ "\0";

           // Convert the header string into bytes.
           byte[] headerBytes = header.getBytes();

           // Use a ByteArrayOutputStream to easily combine the header and file bytes.
           ByteArrayOutputStream blobStream = new ByteArrayOutputStream();
           blobStream.write(headerBytes);
           blobStream.write(fileBytes);

           // This is your final blob data, ready for hashing.
           byte[] blobBytes = blobStream.toByteArray();

           // 4) Calculate the hash.
           // First, let's get Java's built-in fingerprint machine, telling it to use the "SHA-1" algorithm.
           MessageDigest digest = MessageDigest.getInstance("SHA-1");
           // Now, give it our blob data. It returns the raw, 20-byte fingerprint.
           byte[] hash = digest.digest(blobBytes);

           // The raw fingerprint isn't readable, so let's convert it to the 40-character hex string we're used to seeing.
           // Let's create an empty string that we can add to, piece by piece.
           StringBuilder hexString = new StringBuilder();
           // We'll look at each of the 20 bytes from the raw fingerprint, one by one.
           for (byte b : hash) {
             // For each byte, convert it into a 2-digit hex code (like '0a' or 'ff') and add it to our string.
             hexString.append(String.format("%02x", b));
           }
           // And here's our final, beautiful 40-character hash, ready to be used!
           String SHA_1 = hexString.toString();

           // 5) If the "-w" (write) flag is present, compress and write the blob to the .git/objects directory.
           // Check if the user provided the "-w" flag.
           if (flag != null && "-w".equals(flag)) {
             // A. Determine the storage path from the hash.
             // Get the first two characters of the hash for the directory name (e.g., "b5").
             String dirName = SHA_1.substring(0, 2);
             // Get the remaining 38 characters of the hash for the filename.
             String fileName = SHA_1.substring(2);
             // Prepare the path to the directory where the object will be stored (e.g., ".git/objects/b5").
             Path objectDir = Paths.get(".git", "objects", dirName);
             // Prepare the full path to the final object file itself.
             Path objectPath = objectDir.resolve(fileName);

             // B. Create the directory if it doesn't exist.
             // We check if the file exists to avoid trying to create the directory unnecessarily.
             if (Files.notExists(objectPath)) {
               // This command creates the directory and any parent folders if needed.
               Files.createDirectories(objectDir);
             }

             // C. Compress the blob data using the ZLIB algorithm (what Git uses).
             // Create a new Deflater object, which is Java's engine for ZLIB compression.
             Deflater deflater = new Deflater();
             // Give the deflater the uncompressed blob data (header + content).
             deflater.setInput(blobBytes);
             // Tell the deflater that this is all the data it's going to get.
             deflater.finish();

             // Create a "bucket" to hold the compressed data as we generate it.
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             // Create a temporary buffer to hold chunks of compressed data.
             byte[] buffer = new byte[1024];
             // Loop until the deflater has finished compressing all the input data.
             while (!deflater.finished()) {
               // Compress a chunk of data into the buffer and get the number of bytes compressed.
               int count = deflater.deflate(buffer);
               // Write the valid compressed chunk from the buffer into our main output "bucket".
               outputStream.write(buffer, 0, count);
             }
             // Clean up and release any resources used by the deflater.
             deflater.end();
             // Get the final, complete compressed data as a single byte array.
             byte[] compressedBytes = outputStream.toByteArray();

             // D. Write the compressed data to the final file path.
             Files.write(objectPath, compressedBytes);

           } // End of the if-block for the -w flag.

           // Finally, always print the calculated 40-character hash to the console.
           System.out.println(SHA_1);
         }catch (IOException e){
           throw new RuntimeException(e);
         }catch (NoSuchAlgorithmException e) {
             throw new RuntimeException(e);
         }
       }
       // If the command is not "init", run this block of code
       default -> System.out.println("Unknown command: " + command);
     }
  }
}