import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class Main {
  public static void main(String[] args){
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
           // creating an inflater object
           Inflater inflater = new Inflater();
           inflater.setInput(compressedBytes);
           ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
           byte[] buffer = new byte[1024];
           while(!inflater.finished()){
             int count = inflater.inflate(buffer);
             outputStream.write(buffer, 0, count);
           }
           inflater.end();
           byte[] decompressedBytes = outputStream.toByteArray();

           // 4) Find the null-byte separator between the header and content
           int nullByteIndex = -1;
           for(int i=0; i<decompressedBytes.length; i++){
             if (decompressedBytes[i] == 0) {
               nullByteIndex = i;
               break;
             }
           }

           // 5) Extract the content after the separator and print it
           String content = new String(decompressedBytes, nullByteIndex + 1, decompressedBytes.length - (nullByteIndex + 1));
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
       // If the command is not "init", run this block of code
       default -> System.out.println("Unknown command: " + command);
     }
  }
}