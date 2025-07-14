#!/usr/bin/env bash

set -e

adb uninstall org.ameelio.pdfviewer

adb install ameelio-pdf-viewer.apk
