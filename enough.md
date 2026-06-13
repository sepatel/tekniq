# Tekniq Enhancement Summary

## Version 0.22.4-SNAPSHOT

### Module Structure

| Module | Dependencies | Purpose |
|--------|--------------|---------|
| tekniq-core | Kotlin stdlib only | Core libraries (validation, crypto, tracking, basic config) |
| tekniq-config | tekniq-core + Jackson + SnakeYAML | Extended config (JSON, YAML, watched, synchronized) |
| tekniq-cache | tekniq-core + Caffeine | Caching |
| tekniq-jdbc | tekniq-core | JDBC extensions |
| tekniq-rest | tekniq-core + Jackson | REST client |

---

## Validation Reliability

### Fix: notBlank() Logic
Fixed inverted check logic - previously returned `true` (pass) when blank, now correctly fails.

```kotlin
fun notBlank(field: String? = null, ...): TqCheck
```

### Feature: Custom Constraints
Added extensible constraint registration via predicate:

```kotlin
TqCheck(obj).custom("Even") { (it as Int) % 2 == 0 }
TqCheck(obj).custom("Positive", field = "value") { (it as Int) > 0 }
```

---

## Config Reload Semantics

### Fix: TqMapConfig/TqPropertiesConfig
Fixed reload() to properly update backing store. Removed keys stay removed.

### New: tekniq-config Module

```kotlin
// JSON config
TqJsonConfig("classpath:app.json")
TqJsonConfig("/etc/app.json")

// YAML config
TqYamlConfig("classpath:app.yaml")
TqYamlConfig("/etc/app.yaml")

// Thread-safe wrapper
TqSynchronizedConfig(delegate)

// Auto-reload on file change
TqWatchedConfig(config, "/etc/app.properties").startWatching()
```

### Tests: tekniq-config

| Spec | Tests | Coverage |
|------|-------|----------|
| TqJsonConfigSpec | 10 | File loading, classpath, reload, stopOnFailure |
| TqYamlConfigSpec | 10 | File loading, classpath, reload, stopOnFailure |
| TqSynchronizedConfigSpec | 9 | Thread safety, concurrent reads, write locking |
| TqWatchedConfigSpec | 5 | File watching, classpath ignored, lifecycle |

---

## Crypto

### Digital Signatures (intentional breaking change)
`sign`/`verify` now produce/verify proper `SHA256withRSA` digital signatures. Previously they (ab)used raw
RSA as a cipher, which is not a real signature scheme. The API shape changed accordingly:
```kotlin
val signature = TqCryptography.sign(message, keyPair.privateKey)            // ByteArray; String overload returns Base64
val ok: Boolean = TqCryptography.verify(message, signature, keyPair.publicKey)
// infix-style: keyPair.sign(message) ; keyPair.verify(message, signature)
```
`verify` now returns a `Boolean` and requires the original message **plus** the signature. Signatures are
not wire-compatible with prior releases.

### AES-GCM Symmetric (additive)
```kotlin
TqCryptography.aesGcmEncrypt(plaintext, key)
TqCryptography.aesGcmDecrypt(ciphertext, key)
```

RSA `encrypt`/`decrypt` (OAEP-MD5), `md5(...)`, and `TqTrackingType` are unchanged from prior releases.

---

## Build

```bash
./gradlew build      # All modules (34 tests passing)
./gradlew test       # Run all tests
./gradlew :tekniq-core:build  # Core only (no external deps)
```

---

## Implementation Notes

### Config Key Access
Keys are top-level only. Nested access requires:
```kotlin
val db = config.get<Map<*, *>>("database")
db?.get("host")  // NOT config.get("database.host")
```

### WatchedConfig Requirements
- Watches `.json` or `.yaml` files for reload triggers
- Classpath paths are gracefully ignored (no-op)
- Uses background thread with `Executors.newSingleThreadExecutor()`