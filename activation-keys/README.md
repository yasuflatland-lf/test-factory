# Activation Keys

Place your Liferay DXP activation key XML file here before running DXP integration tests.

Source file (Windows/WSL2 users):
  C:\Users\yasuf\Dropbox\Liferay\share\liferay\activation-key-development-7.0de-liferaycom.xml

Copy command (from WSL2 terminal):
  cp /mnt/c/Users/yasuf/Dropbox/Liferay/share/liferay/activation-key-development-7.0de-liferaycom.xml \
     activation-keys/

The integration test container automatically deploys any *.xml file found in this directory
to /opt/liferay/deploy/ at container startup (DXP builds only).

For CI (GitHub Actions): set secret LDF_DXP_ACTIVATION_KEY to the raw XML content.
The workflow step writes it to activation-keys/activation-key.xml automatically.
