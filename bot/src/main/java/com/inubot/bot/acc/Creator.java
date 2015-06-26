package com.inubot.bot.acc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author Septron
 * @since June 22, 2015
 */
public class Creator {

    private static final String PAGE = "https://secure.runescape.com/m=account-creation/g=oldscape/";

    private static final Logger logger = LoggerFactory.getLogger(Creator.class);

    private static URLConnection connection(String address) throws IOException {
        URLConnection connection = new URL(address).openConnection();
        connection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) " +
                        "Gecko/20100316 Firefox/3.6.2");
        return connection;
    }

    public static boolean create(String email, String username, String password) {
        if (username.length() > 12) {
            logger.info("Username too long!");
            return false;
        }

        String[][] post = {
                { "age",                "18"        },
                { "email1",             email       },
                { "displayname",        username    },
                { "password1",          password    },
                { "onlyOneEmail",       "1"         },
                { "onlyOnePassword",    "1"         },
                { "agree_pp_and_tac",   "1"         },
                { "submit",             "register"  },
        };
        try {
            URLConnection connection = connection(PAGE);
            connection.setDoOutput(true);
            try (OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream())) {
                for (String[] var : post) {
                    out.write(var[0] + "=" + var[1] + "&");
                }
                out.flush();
            }

            boolean failed = false;
            String line;
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()))) {
                while ((line = br.readLine()) != null) {
                    if (line.contains("Sorry, you are not eligible to play.")
                            || line.contains("Sorry, you have entered an invalid age.")
                            || line.contains("Please fill out this field.")) {
                        failed = true;
                    }
                    if(line.contains("blocked from creating too many")) {
                        logger.info("Runescape has blocked for creating too many accounts.");
                        logger.info("Please wait an hour or activate a proxy and then continue.");
                    }
                }
                if (failed) {
                    logger.info("Failed to create account : " + username);
                    return false;
                } else {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
