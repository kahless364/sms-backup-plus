package com.zegoggles.smssync.auth;

import android.util.Log;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.zegoggles.smssync.App.TAG;

/**
 * TRANSITIONAL legacy fallback implementation of {@link ContactsPort}.
 *
 * <p>This adapter is an exact verbatim transplant of the {@code getUsernameFromContacts}
 * method and the {@code FeedHandler} inner SAX class that were removed from
 * {@code OAuth2Client} as part of U-029. It preserves the GData Contacts Atom-feed
 * resolution path intact so that binding this adapter in place of
 * {@link PeopleApiContactsAdapter} restores exactly the previous behavior, without
 * any other change (DES-MODERNIZATION-011 §Reversibility; REQ-MODERNIZATION-011
 * Constraints).
 *
 * <p><strong>This class is scheduled for deletion</strong> once People API
 * resolution has been verified end-to-end with a live Google account. It must not
 * be used in production code paths other than the explicit binding-flip path.
 * Delete alongside {@code app/src/test/resources/contacts.xml} in the follow-on
 * cleanup pass.
 *
 * <p>The GData Contacts API and its legacy scope are deprecated and being withdrawn
 * by Google — using this adapter in production will fail for new OAuth grants
 * (DES-MODERNIZATION-011 §Context).
 */
public class GDataContactsAdapter implements ContactsPort {

    private static final String CONTACTS_URL =
            "https://www.google.com/m8/feeds/contacts/default/thin?max-results=1";

    private static final String ERROR = "error";

    /**
     * {@inheritDoc}
     *
     * <p>Issues a GET to the GData Contacts thin feed with the bearer token, SAX-parses
     * the Atom {@code <author><email>} element, and returns the trimmed value.
     * Returns {@code null} on any failure (CNTR-MODERNIZATION-008 VR-2), preserving
     * the identical null-on-exception semantics of the original
     * {@code OAuth2Client.getUsernameFromContacts} (verified lines 227-236).
     */
    @Override
    public String resolveAccountEmail(String accessToken) {
        try {
            HttpsURLConnection connection =
                    (HttpsURLConnection) new URL(CONTACTS_URL).openConnection();
            connection.addRequestProperty("Authorization", "Bearer " + accessToken);
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                final InputStream inputStream = connection.getInputStream();
                String email = extractEmail(inputStream);
                inputStream.close();
                return email;
            } else {
                Log.w(TAG, String.format("GDataContactsAdapter: unexpected server response: %d (%s)",
                        connection.getResponseCode(), connection.getResponseMessage()));
                return null;
            }
        } catch (SAXException e) {
            Log.e(TAG, ERROR, e);
            return null;
        } catch (IOException e) {
            Log.e(TAG, ERROR, e);
            return null;
        } catch (ParserConfigurationException e) {
            Log.e(TAG, ERROR, e);
            return null;
        }
    }

    private String extractEmail(InputStream inputStream)
            throws ParserConfigurationException, SAXException, IOException {
        final XMLReader xmlReader =
                SAXParserFactory.newInstance().newSAXParser().getXMLReader();
        final FeedHandler feedHandler = new FeedHandler();
        xmlReader.setContentHandler(feedHandler);
        xmlReader.parse(new InputSource(inputStream));
        return feedHandler.getEmail();
    }

    /**
     * SAX content handler that extracts the {@code <email>} text inside the Atom
     * {@code <author>} element. Verbatim transplant of {@code OAuth2Client.FeedHandler}
     * (original lines 247-288).
     */
    private static class FeedHandler extends DefaultHandler {
        private static final String EMAIL = "email";
        private static final String AUTHOR = "author";
        private final StringBuilder email = new StringBuilder();
        private boolean inEmail;
        private boolean inAuthor;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) {
            inEmail = EMAIL.equals(qName);
            if (AUTHOR.equals(qName)) {
                inAuthor = true;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (inAuthor && AUTHOR.equals(qName)) {
                inAuthor = false;
            }
        }

        @Override
        public void characters(char[] c, int start, int length) {
            if (inAuthor && inEmail) {
                email.append(c, start, length);
            }
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
            Log.e(TAG, "GDataContactsAdapter: error during parsing", e);
        }

        @Override
        public void warning(SAXParseException e) throws SAXException {
            Log.w(TAG, "GDataContactsAdapter: warning during parsing", e);
        }

        public String getEmail() {
            final String result = email.toString().trim();
            return result.isEmpty() ? null : result;
        }
    }
}
