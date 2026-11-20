#!/usr/bin/env bash
# U-063 throwaway test-fixture generator. JDK keytool only. Test-only material; throwaway keys.
# The store password is a test-only literal.
set -euo pipefail
G="$1"; mkdir -p "$G"
KT="$JDK25/bin/keytool"
P='u063-test-only'
KEY=(-keyalg RSA -keysize 2048 -sigalg SHA256withRSA -storetype PKCS12 -storepass "$P" -keypass "$P")
ST=(-storetype PKCS12 -storepass "$P")
ca() {   # alias dname
  "$KT" -genkeypair -alias "$1" -dname "$2" "${KEY[@]}" -keystore "$G/$1.p12" \
        -startdate "2019/12/01 00:00:00" -validity 36500 \
        -ext bc:c=ca:true,pathlen:0 -ext ku:c=keyCertSign,cRLSign
  "$KT" -exportcert -alias "$1" -keystore "$G/$1.p12" "${ST[@]}" -rfc -file "$G/$1.crt"
}
leaf() { # id host issuer startdate validity-days
  "$KT" -genkeypair -alias "$1" -dname "CN=$2, O=U-063 Test Only" "${KEY[@]}" -keystore "$G/$1.p12" \
        -startdate "$4" -validity "$5"
  "$KT" -certreq -alias "$1" -keystore "$G/$1.p12" "${ST[@]}" -file "$G/$1.csr"
  "$KT" -gencert -alias "$3" -keystore "$G/$3.p12" "${ST[@]}" -sigalg SHA256withRSA \
        -infile "$G/$1.csr" -outfile "$G/$1.crt" -rfc -startdate "$4" -validity "$5" \
        -ext san=dns:"$2" -ext ku:c=digitalSignature,keyEncipherment -ext eku=serverAuth -ext bc=ca:false
  "$KT" -importcert -alias "$3" -file "$G/$3.crt" -keystore "$G/$1.p12" "${ST[@]}" -noprompt
  "$KT" -importcert -alias "$1" -file "$G/$1.crt" -keystore "$G/$1.p12" "${ST[@]}" -noprompt
}
ca   trusted-ca     "CN=U-063 Test Trusted CA, O=U-063 Test Only"
ca   untrusted-ca   "CN=U-063 Test Untrusted CA, O=U-063 Test Only"
leaf right-host     imap.right.test trusted-ca   "2020/01/01 00:00:00" 32000
leaf other-host     imap.other.test trusted-ca   "2020/01/01 00:00:00" 32000
leaf untrusted-host imap.right.test untrusted-ca "2020/01/01 00:00:00" 32000
leaf expired-host   imap.right.test trusted-ca   "2020/01/01 00:00:00" 30
echo "U063-GEN done"
