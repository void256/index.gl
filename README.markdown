# http://index.gl/ - The OpenGL Index

## What is this?

It's a searchable Index of all OpenGL functions and enums available in the new official (XML based API-
Registry)[https://www.opengl.org/discussion_boards/showthread.php/181927-New-XML-based-API-Registry-released]
with the only purpose of displaying the OpenGL version where the function/enum is available.

It shows OpenGL, OpenGL Core Profile and OpenGL ES. So if you are unsure in which OpenGL version, f.i. `glUseProgram`
was introduced - look no further! You've found the right place.

## Limitations

Currently there is no support for Extensions.

Additionally it would be nice to link to the reference pages. This is missing as well.

It is currently hosted run@cloudbees using their free option. So bandwidth might be limited and if the service is
not used it goes into sleep mode which will add a little delay if it is accessed for the first time.

## Web-API

All the search capabilites are exposed in a simple HTTP-based API. So you don't have to use our little html/js based
frontend.

