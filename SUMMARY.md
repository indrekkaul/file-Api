# Summary

This application is a file API that allows users to upload, retrieve, and delete files.

## How to Start the App

To start the application, follow these steps:

1. Open a terminal and navigate to the project directory.
2. Start-up the database `docker-compose up -d`
3. Run the command `mvn spring-boot:run` to start the application.
4. For API documentation go to http://localhost:6011/docs
5. To make requests to the API use a tool like Postman

## Comments

Assignment was to create API what allows uploading files, storing them with metadata 
and accessing or deleting them with a unique token (UUID). Assignment was quite easy to understand.

## Which part of the assignment took the most time and why?

I encountered a challenge regarding the usage of the @NotBlank and @Valid annotations for field validation. These 
annotations are commonly used in Java. However, when I attempted to use these annotations in Kotlin, they did not 
work as I expected. I spent a significant amount of time searching for a solution, but unfortunately, I was unable 
to find a straightforward resolution. To overcome this, I decided to implement the field validation manually.

## What You learned

I really learned the basics about Kotlin

## TODOs

- Limit the File Size in uploads.
- Implement expiration Date logic. Currently, it is not used.
- Add some File Search possibilities.
- Introduce File Versioning?
- Introduce the User Permissions to the Files. User is already validated.
- Consider making every request asynchronous to improve the responsiveness.

