# CMSC436 Google Sheets Common API

This is an Android Library that will allow you to write to the centralized
Google spreadsheet. It will create an Android Archive, or `aar` file (`aar`
files are the Android equivalent of `jar` files) which you can include as a
dependency in your own Android project. Alternatively, you can just copy over
the source code as its own module, though this is not recommended since it will
be harder to pull in centralized changes.

## Useful Readings

1. Read the [Android Quickstart for Google Sheets][quickstart]. Reread it. Pay
attention to the parts where you'll need to get an API token and a fingerprint.

2. Read about how to [sign your app][signing]. This part is optional, but if you
choose to create a release build, you'll have to sign the hash of your release
key.

3. Read the tutorial on creating an [Android Library][library]. This will tell
you how to properly import the Sheets code.

4. Check out the [Google Developer Console][console]. This is where you'll
register any API keys. Depending on how many apps you'll write you may see this
page only once or several times.

## Setting up Authentication

## Setting up the Build

## Importing the Library

1. Download the `aar` from the releases or clone this repository. Downloading
the binary is probably easier, though using the source directly as a library
works too (and might work better if you get confused and end up directly pulling
the code into your own `java` file).

2. Import the library as a module. I'll go over importing it as an `aar`, though
importing the source code isn't terribly difficult either. You'll want to go to
`File -> New -> New Module...` and choose `import .JAR/.AAR Package`. This
should make the library part of your app.

![New Module](images/new_module.png)

![Import Package](images/import_package.png)

Your main module will still need to
list the library as a dependency, so right click on the `app` in the `Android`
view and select `Open Module Settings`. From there, add a `Module Dependency` to
the app.

![Module Settings](images/module_settings.png)

![Module Dependency](images/module_dependency.png)


## Usage

The library abstracts the logic to write to Google Sheets. You need to provide
the spreadsheet's ID, your user ID, the appropriate test, and the data you'd
like to write. To use the `Sheets` class, you'll need to have your calling
activity implement the `Sheets.Host` interface. In addition, you'll want to
forward the `onRequestPermissionsResult` and `onActivityResult` callbacks.

For example, here is a snippet to post data to the left hand tapping test.

```java
import com.example.sheets436.Sheets;

...

private void sendToSheets() {
  String spreadsheetId = "1ASIF7kZHFFaUNiBndhPKTGYaQgTEbqPNfYO5DVb1Y9Y";
  String userId = "t99p99";
  float data = 1.23f;

  Sheets sheet = new Sheets(this, getString(R.string.app_name), spreadsheetId);
  sheet.writeData(Sheets.TestType.LH_TAP, userId, data);
}
```

The `Sheets.TestType` parameter is an enum that will represent the type of test.
If there are more apps to be implemented in the future, we'll add to this enum.

```java
public enum TestType {
  LH_TAP("'Tapping Test (LH)'"),
  RH_TAP("'Tapping Test (RH)'"),
  LH_SPIRAL("'Spiral Test (LH)'"),
  RH_SPIRAL("'Spiral Test (RH)'"),
  LH_LEVEL("'Level Test (LH)'"),
  RH_LEVEL("'Level Test (RH)'"),
  LH_POP("'Balloon Test (LH)'"),
  RH_POP("'Balloon Test (RH)'"),
  LH_CURL("'Curling Test (LH)'"),
  RH_CURL("'Curling Test (RH)'");

  ...
}
```

### Callbacks

Your activity must implement the `Sheets.Host` interface, which requires you to
implement `getRequestCode` and `notifyFinished`.

```java
public interface Host {

  int getRequestCode(Action action);

  void notifyFinished(Exception e);
}
```

The `getRequestCode` method requires you to define unique request codes for the
four different request actions.

```java
public enum Action {
  REQUEST_PERMISSIONS,
  REQUEST_ACCOUNT_NAME,
  REQUEST_PLAY_SERVICES,
  REQUEST_AUTHORIZATION
}
```

The `notifyFinished` method is a callback that triggers upon the write
finishing. You may use this for your own convenience.

### Additional Overrides

You'll want to override `onRequestPermissionsResult` and `onActivityResult` to
forward the implementation onto the `Sheets` class. Your code should be simple.

```java
@Override
public void onRequestPermissionsResult (int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
  this.sheet.onRequestPermissionsResult(requestCode, permissions, grantResults);
}

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
  super.onActivityResult(requestCode, resultCode, data);
  this.sheet.onActivityResult(requestCode, resultCode, data);
}
```

[quickstart]: <https://developers.google.com/sheets/api/quickstart/android>
[signing]: <https://developer.android.com/studio/publish/app-signing.html>
[library]: <https://developer.android.com/studio/projects/android-library.html>
[console]: <https://console.developers.google.com/apis/credentials>
