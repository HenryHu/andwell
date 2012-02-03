=======
AndWell
=======

Android client for accessing BBS with its API.

Build
-----

You may find that it fails to build.
Because you need to create Defs.java by hand.

File content:

package org.net9.andwell;

public class Defs {
    public static final String OAuthClientID = "<OAuth client id>";
    public static final String OAuthClientSecret = "<OAuth client secret>";
}

