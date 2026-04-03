# clojure-cobertura-coverage

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
| Clojure | 1.11+   |
| Leiningen | 2.x   |
| cloverage / lein-cloverage | 1.2.x |

---

## Installation

Add the library to your project's `:dependencies` (or `:dev` profile):

```clojure
;; project.clj
:dependencies [[tooooolong/clojure-cobertura-coverage "0.1.0"]]
:profiles {:dev {:plugins [[lein-cloverage "1.2.4"]]}}
```

---

## Usage

### Via the command line

```bash
lein cloverage --custom-report cloverage.coverage.cobertura/report
```

The report is written to `target/coverage/cobertura.xml` (or whatever
directory you have configured as cloverage's `:output`).

### Via `project.clj`

```clojure
:cloverage {:output        "target/coverage"
            :custom-report cloverage.coverage.cobertura/report}
```

Then run the normal coverage command:

```bash
lein cloverage
```

### Combining with built-in reporters

The `--custom-report` flag is additive — Cloverage still runs all the
reporters you enable (HTML, text, etc.) in addition to the custom one:

```bash
lein cloverage --html --custom-report cloverage.coverage.cobertura/report
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
lein cloverage --custom-report cloverage.coverage.cobertura/report
```

After the run, inspect `target/coverage/cobertura.xml`.

---

## CI/CD integration

### GitLab CI

```yaml
test:
  script:
    - lein cloverage --custom-report cloverage.coverage.cobertura/report
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
        sh 'lein cloverage --custom-report cloverage.coverage.cobertura/report'
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
- name: Run tests with coverage
  run: lein cloverage --custom-report cloverage.coverage.cobertura/report

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

## License

Copyright © 2024 tooooolong

Distributed under the [Eclipse Public License 2.0](LICENSE).
