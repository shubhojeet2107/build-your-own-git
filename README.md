[![progress-banner](https://backend.codecrafters.io/progress/git/d2bf923a-4615-4de5-b80f-4728c2528726)](https://app.codecrafters.io/users/codecrafters-bot?r=2qF)

This is a starting point for Java solutions to the
["Build Your Own Git" Challenge](https://codecrafters.io/challenges/git).

# Build Your Own Git (in Java)

This project is a custom implementation of the Git version control system, built from scratch in Java. The primary goal is to gain a deeper understanding of Git's internal architecture, including its object model, content-addressable storage, and core commands.

This repository is my implementation of the popular ["Build Your Own Git"](https://codecrafters.io/challenges/git) challenge by Codecrafters.

---

## Implemented Features

So far, this project supports the following Git commands:

* **`init`**: Creates the `.git` directory with its essential `objects` and `refs` subdirectories to initialize a new repository.
* **`cat-file`**: Reads and decompresses a Git "blob" object from the `.git/objects` database to display its content.
* **`hash-object`**: Computes the SHA-1 hash for a file, creates a "blob" object with the correct header, and optionally writes it to the object database.

---

## How to Run & Test

You can run the different commands by changing the program arguments in the IntelliJ Run Configuration.

### Initializing the Repository (`init`)
1.  In IntelliJ, go to **Run > Edit Configurations...**.
2.  In the **Program arguments** field, type:
    ```
    init
    ```
3.  Click **Apply**, then **OK**, and run the program.

### Inspecting an Object (`cat-file`)
To test the `cat-file` command, you first need a valid Git object to inspect.

1.  **Create a test object:** Open the IntelliJ Terminal and run the following commands:
    ```bash
    # Create a sample file
    echo "hello world" > test.txt

    # Create a git object from the file and get its hash
    git hash-object -w test.txt
    ```
2.  **Copy the hash:** The `hash-object` command will output a 40-character hash. Copy this hash.

3.  **Update Run Configuration:** Go back to **Run > Edit Configurations...**.

4.  **Set Program Arguments:** In the **Program arguments** field, type the following, replacing `<paste_your_hash_here>` with the hash you just copied:
    ```
    cat-file -p <paste_your_hash_here>
    ```
5.  **Run:** Click **Apply**, then **OK**, and run the program. The output should be `hello world`.

### Creating an Object (`hash-object`)
You can test both hashing and writing an object.

1.  **Update Run Configuration:** Go to **Run > Edit Configurations...**.
2.  **Set Program Arguments:**
    * To only get the hash: `hash-object test.txt`
    * To get the hash **and** write the object to the database: `hash-object -w test.txt`
3.  **Run:** Click **Apply**, then **OK**, and run the program. /tmp/testing
