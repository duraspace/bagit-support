# bagit-support
Libraries to provide support for the use of Bagit bags and Bagit Profiles

[![LICENSE](https://img.shields.io/badge/license-Apache-blue.svg?style=flat-square)](./LICENSE)
[![codecov](https://codecov.io/gh/duraspace/bagit-support/branch/master/graph/badge.svg)](https://codecov.io/gh/duraspace/bagit-support)

## Bag Profiles

The BagIt Support library complies with version `1.3.0` of the Bagit profiles specification and includes the following
bag profiles by default:

* [default](src/main/resources/profiles/default.json)
* [aptrust](src/main/resources/profiles/aptrust.json)
* [beyond the repository](src/main/resources/profiles/beyondtherepository.json)
* [meta archive](src/main/resources/profiles/metaarchive.json)
* [perseids](src/main/resources/profiles/perseids.json)

Because these profiles are built in, we do our best to keep them up to date but they may occasionally need to be 
updated.

### BagIt Profile Json

The BagIt Support library uses bagit profiles which have json compliant to that of the bagit profiles spec. In order to
support constraints on custom tag files, a section called `Other-Info` is used to provide additional contraints. The 
`Other-Info` section is composed of a list of json objects, each of which should be titled for the "tag" file, e.g. 
`APTrust-Info`, and have each of its fields outlined which share the same parameter types as in the `Bag-Info` section.

*Aptrust Other-Info*
```json
"Other-Info" : [{
  "APTrust-Info": {
    "Title": {
      "required": true,
      "description": "The title to be used"
    },
    "Access": {
      "required": true,
      "values": ["Consortia", "Institution", "Restricted"]
    },
    "Storage-Option": {
      "required": true,
      "values": [
        "Standard",
        "Glacier-OH",
        "Glacier-OR",
        "Glacier-VA",
        "Glacier-Deep-OH",
        "Glacier-Deep-OR",
        "Glacier-Deep-VA"
      ]
    }
  }
}]
```

### Using A Built In Profile

The `BagProfile.java` has a default constructor which takes an `InputStream`. This is intended to be the json content
of the Bagit profile being used. As each profile is provided as a resource in the classpath of the jar, it needs to be
referenced in order to be loaded.

*e.g. Using a Built In Profile*
```java
final String profileIdentifier = "beyondtherepository";
final BagProfile.BuiltIn builtInProfile = BagProfile.BuiltIn.from(profileIdentifier);
final BagProfile profile = new BagProfile(builtInProfile);
```

### Loading A Custom Bag Profile

As mentioned above, the `BagProfile` constructor only takes an `InputStream`, so if you want to use a custom Bagit 
Profile, all you need to do is provide the `InputStream` for your json schema.

```java
final BagProfile profile;
final Path json = Paths.get("/profiles/bagit-profile.json");
try (InputStream is = Files.newInputStream(json)) {
    profile = new BagProfile(is);
}
```

### Validating A Bag

The `BagProfile` has the capabilities to validate that a `Bag` (read by the gov.loc bagit library) conforms to its
standard. In order to use this validation, the `BagProfile#validate(Bag)` should be uesd. If validation fails, a
`RuntimeException` is thrown describing what sections failed to validate.

```java
final Path bag = Paths.get("/bags/my-really-cool-bag");
final BagReader reader = new BagReader();
try {
    final Bag readBag = reader.read(bag);
    profile.validateBag(readBag);
} catch (UnparsableVersionException | MaliciousPathException | UnsupportedAlgorithmException |
         InvalidBagitFileFormatException e) {
    log.error("Unable to read bag", e);
}
```

### Validating A BagConfig

In addition to the validation on a Bag, a `BagProfile` can also validate a `BagConfig` before the process of writing
begins in order to verify that all the tag files used will be compliant with a given `BagProfile`. If a `BagConfig` 
fails to validate, a `RuntimeException` is thrown.

```java
final Path yaml = Paths.get("/config/sample-bag.yml");
final BagConfig config = new BagConfig(yaml.toFile());
profile.validateConfig(config);
```

## Bag Writing

In order to help write Bagit bags, a basic `BagWriter` is provided which only writes the metadata (tag files, 
manifests) of a bag. It requires the user to populate the payload files for a bag as well as track what the
checksums are for each payload file. A `bagit.txt` is generated by default but all other tag files must have data 
provided, including the `bag-info.txt`, otherwise they will not be written. The `BagConfig` class can be used to help
assist with loading values for tag files such as the `bag-info.txt`.

The `BagWriter` comes with a few method to help populate tag files for the bag:
```java
public void registerChecksums(final String algorithm, final Map<File, String> filemap)
public void addTags(final String key, final Map<String, String> values)
```

*Writing a Bag*
```java
final Long bytesWritten;
final Long filesWritten;
final Path bag = Paths.get("/bags/sample-bag")
final Path yaml = Paths.get("/config/sample-bag.yml")
final BagItDigest sha1 = BagItDigest.SHA1;
final Map<File, String> sha1Checksums = new HashMap<>();

// work to populate data directory
...

// configure the BagWriter
final BagWriter writer = new BagWriter(bag, Set.of(sha1.bagitName());
writer.registerChecksums(sha1.bagitName(), sha1Checksums());

// register tag files
final BagConfig config = new BagConfig(yaml.toFile());
config.getTagFiles().forEach(filename -> writer.addTags(filename, config.getFieldsForTagFile(filename));

// finish the bag-info.txt with information from populating the data directory
Map<String, String> info = writer.getTags(BagConfig.BAG_INFO_KEY);
Map<String, String> generatedo = Map.of(BagConfig.BAG_SIZE_KEY, byteCountToDisplaySize(bytesWritten), 
                                        BagConfig.PAYLOAD_OXUM_KEY, bytesWritten.toString() + "." + filesWritten.toString(),
                                        BagConfig.BAGGING_DATE_KEY, DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.now())
writer.addTags(BagConfig.BAG_INFO_KEY, info.putAll(generated));
writer.write();
```

*sample-bag.yml*
```yaml
bag-info.txt:
  Source-Organization: org.duraspace
  External-Description: Sample bag
  External-Identifier: SAMPLE_001
  Bag-Group-Identifier: SAMPLE
  Internal-Sender-Identifier: SAMPLE_001
  Internal-Sender-Description: Sample bag
aptrust-info.txt:
  Access: Restricted
  Title: Sample bag
```

## Serialization

The BagIt Support library can assist with serialization and deserialization of Bagit bags. 

Supported formats are:
* zip: zip, application/zip
* tar: tar, application/tar, application/x-tar, application/gtar, application/x-gtar
* gzip (only tar+gz when serializing): tgz, gzip, tar+gzip, application/gzip, application/x-gzip, 
application/x-compressed-tar

Because gzip is a compression/decompression format, when deserializing gzip only decompression occurs. This means that
it will require more space to decompress a tar+gzip bag because it will first decompress the gzip portion, then extract
the tar archive.

The `SerializationSupport` class offers helper methods for instantiating the correct `BagSerializer` or 
`BagDeserializer` depending on what is passed in:

```java
public static BagSerializer serializerFor(final String contentType, final BagProfile profile)
public static BagDeserializer deserializerFor(final Path serializedBag, final BagProfile profile)
```

### Serializing Bags

When retrieving a `BagSerializer`, the correct serializer is created based on the given `contentType` and `BagProfile`.
If the `contentType` is not supported by either the `BagProfile` or the `SerializationSupport` class, a 
`RuntimeException` is thrown.

```java
final Path bag = Paths.get("/bags/my-really-cool-bag");
final String contentType = "zip";
final BagProfile profile = new BagProfile(getProfileInputStream());
final BagSerializer serializer = SerializationSupport.serializerFor(contentType, profile);

final Path serialized = serializer.serialize(bag);
```

### Deserializing Bags

Retrieving the `BagDeserializer` is similar to the `BagSerializer`. When attempting to find the appropriate 
`BagDeserializer` to use, the apache tika library is used in order to read the content type of the `Path`. If a 
`BagProfile` does not support the found content type, a `RuntimeException` is once again thrown, and if the
`SerializationSupport` does not have built in support for the content type, an `UnsupportedOperationException` is 
thrown.

```java
final Path bag = Paths.get("/bags/my-really-cool-bag.tar.gz");
final BagProfile profile = new BagProfile(getProfileInputStream());
final BagSerializer deserializer = SerializationSupport.deserializerFor(bag, profile);

final Path deserialized = deserializer.deserialize(bag);
```
