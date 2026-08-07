## extension-androidtools

![](https://img.shields.io/github/repo-size/MAJigsaw77/extension-androidtools) ![](https://badgen.net/github/open-issues/MAJigsaw77/extension-androidtools) ![](https://badgen.net/badge/license/MIT/green)

A Haxe/[Lime](https://lime.openfl.org) extension that incorporates Java functions through [JNI](https://en.m.wikipedia.org/wiki/Java_Native_Interface).

### Features (This Fork)

This fork includes enhanced **Android Storage Access Framework (SAF)** support, providing a modern and secure way to access documents and files on Android devices. The SAF integration allows your Haxe/Lime applications to:

- **Open Documents** – Let users browse and select files from their device storage or cloud providers via the system file picker.
- **Create Documents** – Save new files to user-chosen locations with custom MIME types and suggested file names.
- **Read & Write Text** – Directly read UTF‑8 text from and write text to content URIs returned by the SAF.
- **Persist URI Permissions** – Retain long‑term access to selected files across app restarts (when supported by the device).

All SAF functions are available through the `android.Tools` class and use standard Android request codes to integrate seamlessly with your activity result callbacks.

### Installation
You can install it through `Haxelib`
```bash
haxelib git extension-androidtools https://github.com/MAJigsaw77/extension-androidtools.git
```

## Licensing

**extension-androidtools** is made available under the **MIT License**. Check [LICENSE](./LICENSE) for more information.
