# Release Guide

This project publishes Android releases as APK assets attached to git tags.

## Manual Release

1. Make sure `main` contains the release commit.
2. Update `versionCode`, `versionName`, and `CHANGELOG.md`.
3. Build the release APK:

```bash
./gradlew :app:clean :app:assembleRelease
```

4. Create and push a tag:

```bash
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0
```

5. Create a release in Gitea or GitHub for the tag.
6. Upload `app/build/outputs/apk/release/app-release.apk` as a release asset.

The platform-generated source archives are for developers. Android users should install the uploaded APK.

## Signing

Release signing values are loaded from `local.properties`:

```properties
IPTV_RELEASE_STORE_FILE=keystore/iptv-release.jks
IPTV_RELEASE_KEY_ALIAS=release
IPTV_RELEASE_STORE_PASSWORD=change-me
IPTV_RELEASE_KEY_PASSWORD=change-me
```

Never commit keystores or real passwords.
