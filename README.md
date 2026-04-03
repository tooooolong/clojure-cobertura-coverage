# clojure-cobertura-coverage

[![CI](https://github.com/tooooolong/clojure-cobertura-coverage/actions/workflows/ci.yml/badge.svg)](https://github.com/tooooolong/clojure-cobertura-coverage/actions/workflows/ci.yml)
[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.tooooolong/clojure-cobertura-coverage.svg)](https://clojars.org/org.clojars.tooooolong/clojure-cobertura-coverage)

A [Cloverage](https://github.com/cloverage/cloverage) custom reporter plugin that outputs
[Cobertura XML](http://cobertura.sourceforge.net/) coverage reports.

Cobertura XML is the de-facto standard consumed by CI/CD systems such as
**GitLab CI**, **Jenkins** (Cobertura Plugin), **GitHub Actions** (coverage
summary actions), **Codecov**, and **SonarQube**.

---

## Requirements

| Tool    | Version |
|---------|---------|
| Java    | 8+      |
| Clojure | 1.10+   |
| Leiningen | 2.x *(optional)* |
| Clojure CLI (`clj`) | 1.11+ *(optional)* |
| cloverage / lein-cloverage | 1.2.x |

---

## Installation

### Leiningen

Add the library to your project's `:dependencies` (or `:dev` profile):

```clojure
;; project.clj
:dependencies [[org.clojars.tooooolong/clojure-cobertura-coverage "0.1.3"]]
:profiles {:dev {:plugins [[lein-cloverage "1.2.4"]]}}
```

### Clojure CLI (deps.edn)

Add a `:coverage` alias to your `deps.edn`:

```clojure
;; deps.edn
{:aliases
 {:coverage
  {:extra-paths ["test"]
   :extra-deps  {cloverage/cloverage {:mvn/version "1.2.4"}
                 org.clojars.tooooolong/clojure-cobertura-coverage
                 {:mvn/version "0.1.3"}}
   :main-opts   ["-m" "cloverage.coverage"
                 "--custom-report" "cloverage.coverage.cobertura/report"
                 "--output" "target/coverage"]}}}
```

---

## Usage

### Leiningen — command line

```bash
lein cloverage --custom-report cloverage.coverage.cobertura/report
```

The report is written to `target/coverage/cobertura.xml` (or whatever
directory you have configured as cloverage's `:output`).

### Leiningen — `project.clj`

```clojure
:cloverage {:output        "target/coverage"
            :custom-report cloverage.coverage.cobertura/report}
```

Then run the normal coverage command:

```bash
lein cloverage
```

### Clojure CLI — `clj -M:coverage`

With the alias defined in `deps.edn` (see Installation above):

```bash
clj -M:coverage
```

To override options on the command line, append them after `--`:

```bash
# Custom output directory
clj -M:coverage --output target/my-coverage

# Also generate the built-in HTML report
clj -M:coverage --html
```

### Combining with built-in reporters

The `--custom-report` flag is additive — Cloverage still runs all the
reporters you enable (HTML, text, etc.) in addition to the custom one:

```bash
# Leiningen
lein cloverage --html --custom-report cloverage.coverage.cobertura/report

# Clojure CLI
clj -M:coverage --html
```

---

## Output format

The generated `cobertura.xml` follows the Cobertura 4 DTD schema:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE coverage SYSTEM "http://cobertura.sourceforge.net/xml/coverage-04.dtd">
<coverage line-rate="0.7500" branch-rate="0.0"
          lines-covered="6" lines-valid="8"
          branches-covered="0" branches-valid="0"
          complexity="0" version="1" timestamp="1712134567">
  <sources>
    <source>src</source>
  </sources>
  <packages>
    <package name="example" line-rate="0.7500" branch-rate="0.0" complexity="0">
      <classes>
        <class name="example.core" filename="example/core.clj"
               line-rate="0.7500" branch-rate="0.0" complexity="0">
          <methods/>
          <lines>
            <line number="7"  hits="3" branch="false"/>
            <line number="12" hits="2" branch="false"/>
            <line number="18" hits="0" branch="false"/>
            ...
          </lines>
        </class>
      </classes>
    </package>
  </packages>
</coverage>
```

### Namespace → Package/Class mapping

| Clojure namespace   | Cobertura package | Cobertura class     |
|---------------------|-------------------|---------------------|
| `example.core`      | `example`         | `example.core`      |
| `example.util.str`  | `example.util`    | `example.util.str`  |
| `core` (top-level)  | `(default)`       | `core`              |

---

## Running the demo

This repository includes an example namespace (`example.core`) that
deliberately leaves two functions (`divide`, `factorial`) untested so you
can see partial coverage in the report.

```bash
# Leiningen
lein cloverage --custom-report cloverage.coverage.cobertura/report

# Clojure CLI (uses the :coverage alias in this repo's deps.edn)
clj -M:coverage
```

After the run, inspect `target/coverage/cobertura.xml`.

---

## CI/CD integration

### GitLab CI

```yaml
test:
  script:
    # Leiningen
    - lein cloverage --custom-report cloverage.coverage.cobertura/report
    # — or Clojure CLI —
    # - clj -M:coverage
  coverage: '/Coverage: (\d+\.\d+)%/'
  artifacts:
    reports:
      coverage_report:
        coverage_format: cobertura
        path: target/coverage/cobertura.xml
```

### Jenkins (Cobertura Plugin)

```groovy
stage('Test') {
    steps {
        // Leiningen
        sh 'lein cloverage --custom-report cloverage.coverage.cobertura/report'
        // — or Clojure CLI —
        // sh 'clj -M:coverage'
    }
    post {
        always {
            cobertura coberturaReportFile: 'target/coverage/cobertura.xml'
        }
    }
}
```

### GitHub Actions

```yaml
- name: Run tests with coverage (Leiningen)
  run: lein cloverage --custom-report cloverage.coverage.cobertura/report
# — or Clojure CLI —
# - name: Run tests with coverage (Clojure CLI)
#   run: clj -M:coverage

- name: Upload coverage report
  uses: actions/upload-artifact@v4
  with:
    name: coverage
    path: target/coverage/cobertura.xml
```

---

## How it works

Cloverage's `--custom-report` option accepts a fully-qualified symbol
(`namespace/function`). After running all instrumented tests, Cloverage
calls the function with a single map:

```clojure
{:output  "target/coverage"   ; output directory
 :forms   [...]               ; raw coverage forms collection
 :args    {...}               ; parsed CLI options
 :project {...}}              ; Leiningen project map
```

This reporter:

1. Calls `cloverage.report/file-stats` on `:forms` to get per-file metrics
   (covered lines, instrumented lines, etc.)
2. Groups the raw forms by file and calls `cloverage.report/line-stats`
   per file to obtain per-line hit counts
3. Builds a Cobertura XML document mapping namespaces → packages/classes
4. Writes `cobertura.xml` to the output directory

---

## Releasing to Clojars

This project uses a GitHub Actions [release workflow](.github/workflows/release.yml) that
deploys to [Clojars](https://clojars.org) automatically when you push a `v*` tag.

### Prerequisites

Add this repository secret in **Settings → Secrets and variables → Actions**:

| Secret | Value |
|--------|-------|
| `CLOJARS_DEPLOY_TOKEN` | A Clojars [deploy token](https://clojars.org/tokens) for the publishing account |

The workflow uses the fixed Clojars username `tooooolong`; the token replaces the
password when deploying.

### Steps to release

```bash
# 1. Ensure main is clean and tests pass
git checkout main
git pull

# 2. Tag the release (the workflow strips the leading 'v')
git tag vX.Y.Z
git push origin vX.Y.Z
```

The workflow will:
1. Extract the version from the tag (`vX.Y.Z` → `X.Y.Z`)
2. Patch `project.clj` with that version in the ephemeral CI workspace
3. Run `lein deploy clojars`

The checked-in `project.clj` can stay on a placeholder development version.
The release workflow rewrites it in CI from the tag you push, so the published
artifact version always matches the release tag.

---

## License

Distributed under the [Eclipse Public License 2.0](LICENSE).
