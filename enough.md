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

## Crypto (rewritten — breaking)

Asymmetric keys moved out of `TqCryptography` onto a single sealed `TqKeyPair` covering both RSA and Ed25519.
`TqCryptography` now holds only the keyless primitives.

### Unified key pair
Key material is a Base64 DER string instead of `BigInteger` modulus/exponent pairs, so RSA and Ed25519 persist
and rebuild identically:
```kotlin
val rsa = TqKeyPair.Rsa.generate(bits = 2048)
val ed = TqKeyPair.Ed25519.generate()

fun stamp(key: TqKeyPair, msg: String) = key.sign(msg)   // algorithm-agnostic
```

| Was | Now |
|-----|-----|
| `TqCryptography.generateKeyPair()` | `TqKeyPair.Rsa.generate()` |
| `TqKeyPair.PrivateKey(modulus, exponent)` | `TqPrivateKey.Rsa(pkcs8)` |
| `TqKeyPair.PublicKey(modulus, exponent)` | `TqPublicKey.Rsa(x509)` |
| `TqCryptography.sign(msg, key)` | `key.sign(msg)` |
| `TqCryptography.verify(msg, sig, key)` | `key.verify(msg, sig)` |

Operations live on the keys themselves, so a bare public key can verify without a pair. `encrypt`/`decrypt` are
declared only on the `Rsa` variant — Ed25519 has no encryption scheme, so misuse is a compile error. `verify`
fails closed: a tampered message, corrupt signature or non-Base64 string returns `false` instead of throwing.
Private keys redact themselves from `toString()`.

### Ed25519 (new)
Native JDK Ed25519, verified against the RFC 8032 §7.1 test vector. Includes raw 32-byte key interop in both
directions (`TqPublicKey.Ed25519.ofRaw(bytes)` / `key.raw()`) for SSH, JWK and non-JVM tooling.

### RSA encryption is now hybrid
`encrypt` wraps a single-use AES-256-GCM data key with RSA-OAEP-SHA256. This replaces OAEP-**MD5** plus a
hand-rolled space-delimited Base64 chunking scheme whose block-size math used the PKCS#1 v1.5 formula
(`bits/8 - 11`), which overshoots OAEP's real limit and silently failed past ~222 bytes on a 2048-bit key.
Payloads of any size now work and tampering is detected. Ciphertext is not wire-compatible with prior releases.

### AES-GCM fixed
`aesGcmEncrypt`/`aesGcmDecrypt` passed an `IvParameterSpec`, which SunJCE rejects outright — the functions
threw `InvalidAlgorithmParameterException` on every call and had no test coverage. Now uses `GCMParameterSpec`
with an explicit 128-bit tag, and the 12-byte nonce is prefixed to the ciphertext.

`md5(...)`, `sha256(...)`, `hmac(...)`, the Base64 helpers and `TqTrackingType` are unchanged.

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