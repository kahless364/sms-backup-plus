package com.zegoggles.smssync;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.common.truth.Truth.assertThat;

/**
 * Verifies AC-1, AC-2, and AC-5 for U-056 (SE-004):
 *
 * AC-1: network_security_config.xml exists with cleartextTrafficPermitted="false" base-config;
 *       AndroidManifest.xml references it on the {@code <application>} element.
 * AC-2: android:networkSecurityConfig appears only on the {@code <application>} element,
 *       not on {@code <receiver>}/{@code <service>}/{@code <activity>};
 *       BackupBroadcastReceiver entry is unchanged (CNTR-MODERNIZATION-005).
 * AC-5: Structural assertion that cleartextTrafficPermitted is "false" in the source XML.
 */
public class NetworkSecurityConfigTest {

    private static final String MANIFEST_PATH      = "src/main/AndroidManifest.xml";
    private static final String NETWORK_CONFIG_PATH = "src/main/res/xml/network_security_config.xml";
    private static final String MODULE_DIR          = "app";

    /** Resolve a path relative to the module directory, searching up from CWD. */
    private File resolveModuleFile(String relativePath) {
        // CWD is the project root when run via Gradle :app:testDebugUnitTest
        File candidate = new File(MODULE_DIR, relativePath);
        if (candidate.exists()) return candidate;
        // Fallback: CWD is already inside app/
        candidate = new File(relativePath);
        if (candidate.exists()) return candidate;
        // Try one level up
        return new File("..", MODULE_DIR + "/" + relativePath);
    }

    @Test
    public void networkSecurityConfigXml_shouldExistAndDisableCleartext() throws Exception {
        File configFile = resolveModuleFile(NETWORK_CONFIG_PATH);
        assertWithMessage("network_security_config.xml must exist at " + configFile.getAbsolutePath())
                .that(configFile.exists()).isTrue();

        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = db.parse(configFile);
        doc.getDocumentElement().normalize();

        // Root element must be <network-security-config>
        assertThat(doc.getDocumentElement().getTagName()).isEqualTo("network-security-config");

        // Must have exactly one <base-config> child
        NodeList baseConfigs = doc.getElementsByTagName("base-config");
        assertWithMessage("Expected exactly one <base-config> element")
                .that(baseConfigs.getLength()).isEqualTo(1);

        Element baseConfig = (Element) baseConfigs.item(0);
        String cleartext = baseConfig.getAttribute("cleartextTrafficPermitted");
        assertWithMessage("cleartextTrafficPermitted on <base-config>")
                .that(cleartext).isEqualTo("false");
    }

    @Test
    public void androidManifest_shouldReferenceNetworkSecurityConfigOnApplicationOnly()
            throws Exception {
        File manifestFile = resolveModuleFile(MANIFEST_PATH);
        assertWithMessage("AndroidManifest.xml must exist at " + manifestFile.getAbsolutePath())
                .that(manifestFile.exists()).isTrue();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder db = factory.newDocumentBuilder();
        Document doc = db.parse(manifestFile);
        doc.getDocumentElement().normalize();

        // The <application> element must carry android:networkSecurityConfig
        NodeList appNodes = doc.getElementsByTagName("application");
        assertThat(appNodes.getLength()).isEqualTo(1);
        Element appEl = (Element) appNodes.item(0);
        String nscAttr = appEl.getAttributeNS(
                "http://schemas.android.com/apk/res/android", "networkSecurityConfig");
        assertWithMessage("android:networkSecurityConfig on <application>")
                .that(nscAttr).isEqualTo("@xml/network_security_config");

        // No <receiver>, <service>, or <activity> element should carry networkSecurityConfig
        for (String tag : new String[]{"receiver", "service", "activity"}) {
            NodeList nodes = doc.getElementsByTagName(tag);
            for (int i = 0; i < nodes.getLength(); i++) {
                Element el = (Element) nodes.item(i);
                String val = el.getAttributeNS(
                        "http://schemas.android.com/apk/res/android", "networkSecurityConfig");
                String elName = el.getAttributeNS(
                        "http://schemas.android.com/apk/res/android", "name");
                assertWithMessage(
                        "android:networkSecurityConfig must not appear on <" + tag + "> " + elName)
                        .that(val).isEmpty();
            }
        }
    }

    @Test
    public void androidManifest_backupBroadcastReceiverShouldBeUnchanged() throws Exception {
        File manifestFile = resolveModuleFile(MANIFEST_PATH);
        assertThat(manifestFile.exists()).isTrue();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder db = factory.newDocumentBuilder();
        Document doc = db.parse(manifestFile);
        doc.getDocumentElement().normalize();

        NodeList receivers = doc.getElementsByTagName("receiver");
        Element backupReceiver = null;
        for (int i = 0; i < receivers.getLength(); i++) {
            Element el = (Element) receivers.item(i);
            String name = el.getAttributeNS(
                    "http://schemas.android.com/apk/res/android", "name");
            if (name.contains("BackupBroadcastReceiver")) {
                backupReceiver = el;
                break;
            }
        }

        assertWithMessage("BackupBroadcastReceiver must be present in manifest")
                .that(backupReceiver).isNotNull();

        // exported must be "true" (CNTR-MODERNIZATION-005)
        String exported = backupReceiver.getAttributeNS(
                "http://schemas.android.com/apk/res/android", "exported");
        assertWithMessage("BackupBroadcastReceiver android:exported must be \"true\"")
                .that(exported).isEqualTo("true");

        // No android:permission attribute (CNTR-MODERNIZATION-005 prohibits adding one)
        String permission = backupReceiver.getAttributeNS(
                "http://schemas.android.com/apk/res/android", "permission");
        assertWithMessage("BackupBroadcastReceiver must NOT have android:permission")
                .that(permission).isEmpty();
    }
}
