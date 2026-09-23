# xgradle-sbom-generator

**xgradle-sbom-generator** is a support module for generating SBOM reports from
resolved Gradle dependencies in offline/system-artifact workflows.

## Supported formats

- SPDX JSON
- CycloneDX JSON

## Activation

The module is consumed by `xgradle-resolution-plugin` and is activated by property:

```bash
-Dgenerate.sbom=spdx
-Dgenerate.sbom=cyclonedx
```

You can also put this key in `~/.xgradle/xgradle.config`.

## Output

Generated file is written to:

- `build/reports/xgradle/sbom-spdx.json`
- `build/reports/xgradle/sbom-cyclonedx.json`

## Usage Example:

```bash
gradle build \
  -Dmaven.metadata.dir=/usr/share/maven-metadata \
  -Dgenerate.sbom=cyclonedx \
  --offline
```

```
[root@82c69e916850 plumelib-options]# cat build/reports/xgradle/sbom-cyclonedx.json 
{
  "bomFormat": "CycloneDX",
  "specVersion": "1.5",
  "version": 1,
  "metadata": {
    "timestamp": "2026-03-17T11:16:03.748746500Z",
    "tools": [
      {
        "vendor": "BaseALT",
        "name": "xgradle-sbom-generator",
        "version": "unspecified"
      }
    ],
    "component": {
      "type": "application",
      "name": "options",
      "version": "unspecified"
    }
  },
  "components": [
    {
      "type": "library",
      "group": "com.google.code.findbugs",
      "name": "jsr305",
      "version": "3.0.2",
      "purl": "pkg:maven/com.google.code.findbugs/jsr305@3.0.2",
      "externalReferences": [
        {
          "type": "website",
          "url": "http://findbugs.sourceforge.net/"
        },
        {
          "type": "vcs",
          "url": "https://github.com/amaembo/jsr-305"
        }
      ],
      "licenses": [
        {
          "license": {
            "name": "The BSD 3-Clause License",
            "url": "https://opensource.org/licenses/BSD-3-Clause"
          }
        }
      ]
    },
    {
      "type": "library",
      "group": "com.google.guava",
      "name": "failureaccess",
      "version": "1.0.1",
      "purl": "pkg:maven/com.google.guava/failureaccess@1.0.1",
      "externalReferences": [
        {
          "type": "website",
          "url": "https://github.com/google/guava"
        },
        {
          "type": "vcs",
          "url": "https://github.com/google/guava"
        }
      ],
      "licenses": [
        {
          "license": {
            "name": "Apache License, Version 2.0",
            "url": "http://www.apache.org/licenses/LICENSE-2.0.txt"
          }
        }
      ]
    },
    {
      "type": "library",
      "group": "com.google.guava",
      "name": "guava",
      "version": "31.0.1-jre",
      "purl": "pkg:maven/com.google.guava/guava@31.0.1-jre",
      "externalReferences": [
        {
          "type": "website",
          "url": "https://github.com/google/guava"
        },
        {
          "type": "vcs",
          "url": "https://github.com/google/guava"
        }
      ],
      "licenses": [
        {
          "license": {
            "name": "Apache License, Version 2.0",
            "url": "http://www.apache.org/licenses/LICENSE-2.0.txt"
          }
        }
      ]
    },
    {
      "type": "library",
      "group": "com.univocity",
      "name": "univocity-parsers",
      "version": "2.9.1",
      "purl": "pkg:maven/com.univocity/univocity-parsers@2.9.1",
      "externalReferences": [
        {
          "type": "website",
          "url": "http://github.com/univocity/univocity-parsers"
        },
        {
          "type": "vcs",
          "url": "https://github.com/univocity/univocity-parsers"
        }
      ],
      "licenses": [
        {
          "license": {
            "name": "Apache 2",
            "url": "http://www.apache.org/licenses/LICENSE-2.0.txt"
          }
        }
      ]
    },
    {
      "type": "library",
      "group": "io.github.classgraph",
      "name": "classgraph",
      "version": "4.8.184",
      "purl": "pkg:maven/io.github.classgraph/classgraph@4.8.184",
      "externalReferences": [
        {
          "type": "website",
          "url": "https://github.com/classgraph/classgraph"
        },
        {
          "type": "vcs",
          "url": "https://github.com/classgraph/classgraph"
        }
      ],
      "licenses": [
        {
          "license": {
            "name": "The MIT License (MIT)",
            "url": "http://opensource.org/licenses/MIT"
          }
        }
      ]
    },
    {
      "type": "library",
      "group": "org.apache.commons",
      "name": "commons-lang3",
      "version": "3.19.0",
      "purl": "pkg:maven/org.apache.commons/commons-lang3@3.19.0",
      "externalReferences": [
        {
          "type": "website",
          "url": "https://commons.apache.org/proper/commons-lang/"
        },
        {
          "type": "vcs",
          "url": "https://gitbox.apache.org/repos/asf/commons-lang.git"
        }
      ]
    },
    {
      "type": "library",
      "group": "org.apache.commons",
      "name": "commons-text",
      "version": "1.15.0",
      "purl": "pkg:maven/org.apache.commons/commons-text@1.15.0",
      "externalReferences": [
        {
          "type": "website",
          "url": "https://commons.apache.org/proper/commons-text"
        },
        {
          "type": "vcs",
          "url": "https://gitbox.apache.org/repos/asf?p=commons-text.git"
        }
      ]
    },
    {
      "type": "library",
      "group": "org.apiguardian",
      "name": "apiguardian-api",
      "version": "1.1.2",
      "purl": "pkg:maven/org.apiguardian/apiguardian-api@1.1.2",
      "externalReferences": [
        {
          "type": "website",
          "url": "https://github.com/apiguardian-team/apiguardian"
        },
        {
          "type": "vcs",
          "url": "https://github.com/apiguardian-team/apiguardian"
        }
      ],
      "licenses": [
        {
          "license": {
            "name": "The Apache License, Version 2.0",
            "url": "http://www.apache.org/licenses/LICENSE-2.0.txt"
          }
        }
      ]
    },
    {
      "type": "library",
      "group": "org.checkerframework",
      "name": "checker-qual",
      "version": "3.52.0",
      "purl": "pkg:maven/org.checkerframework/checker-qual@3.52.0",
      "licenses": [
        {
          "license": {
            "name": "The MIT License",
            "url": "http://opensource.org/licenses/MIT"
          }
        }
      ]
    },
    {
      "type": "library",
      "group": "org.junit.jupiter",
      "name": "junit-jupiter-api",
      "version": "5.10.2",
      "purl": "pkg:maven/org.junit.jupiter/junit-jupiter-api@5.10.2",
      "externalReferences": [
        {
          "type": "website",
          "url": "https://junit.org/junit5/"
        },
        {
          "type": "vcs",
          "url": "https://github.com/junit-team/junit5"
        }
      ],
      "licenses": [
        {
          "license": {
            "name": "Eclipse Public License v2.0",
            "url": "https://www.eclipse.org/legal/epl-v20.html"
          }
        }
      ]
    },
    {
      "type": "library",
      "group": "org.junit.jupiter",
      "name": "junit-jupiter-engine",
      "version": "5.10.2",
      "purl": "pkg:maven/org.junit.jupiter/junit-jupiter-engine@5.10.2",
      "externalReferences": [
        {
          "type": "website",
          "url": "https://junit.org/junit5/"
        },
        {
          "type": "vcs",
          "url": "https://github.com/junit-team/junit5"
        }
      ],
      "licenses": [
        {
          "license": {
            "name": "Eclipse Public License v2.0",
            "url": "https://www.eclipse.org/legal/epl-v20.html"
          }
        }
      ]
    },
    {
      "type": "library",
      "group": "org.junit.jupiter",
      "name": "junit-jupiter-params",
      "version": "5.10.2",
      "purl": "pkg:maven/org.junit.jupiter/junit-jupiter-params@5.10.2",
      "externalReferences": [
        {
          "type": "website",
          "url": "https://junit.org/junit5/"
        }
      ]
    },
    {
      "type": "library",
      "group": "org.junit.platform",
      "name": "junit-platform-commons",
      "version": "1.10.2",
      "purl": "pkg:maven/org.junit.platform/junit-platform-commons@1.10.2",
      "externalReferences": [
        {
          "type": "website",
          "url": "https://junit.org/junit5/"
        },
        {
          "type": "vcs",
          "url": "https://github.com/junit-team/junit5"
        }
      ],
      "licenses": [
        {
          "license": {
            "name": "Eclipse Public License v2.0",
            "url": "https://www.eclipse.org/legal/epl-v20.html"
          }
        }
      ]
    },
    {
      "type": "library",
      "group": "org.junit.platform",
      "name": "junit-platform-engine",
      "version": "1.10.2",
      "purl": "pkg:maven/org.junit.platform/junit-platform-engine@1.10.2",
      "externalReferences": [
        {
          "type": "website",
          "url": "https://junit.org/junit5/"
        },
        {
          "type": "vcs",
          "url": "https://github.com/junit-team/junit5"
        }
      ],
      "licenses": [
        {
          "license": {
            "name": "Eclipse Public License v2.0",
            "url": "https://www.eclipse.org/legal/epl-v20.html"
          }
        }
      ]
    },
    {
      "type": "library",
      "group": "org.opentest4j",
      "name": "opentest4j",
      "version": "1.3.0",
      "purl": "pkg:maven/org.opentest4j/opentest4j@1.3.0",
      "externalReferences": [
        {
          "type": "website",
          "url": "https://github.com/ota4j-team/opentest4j"
        },
        {
          "type": "vcs",
          "url": "https://github.com/ota4j-team/opentest4j"
        }
      ],
      "licenses": [
        {
          "license": {
            "name": "The Apache License, Version 2.0",
            "url": "https://www.apache.org/licenses/LICENSE-2.0.txt"
          }
        }
      ]
    },
    {
      "type": "library",
      "group": "org.ow2.asm",
      "name": "asm",
      "version": "9.9",
      "purl": "pkg:maven/org.ow2.asm/asm@9.9",
      "externalReferences": [
        {
          "type": "website",
          "url": "http://asm.ow2.io/"
        },
        {
          "type": "vcs",
          "url": "https://gitlab.ow2.org/asm/asm/"
        }
      ],
      "licenses": [
        {
          "license": {
            "name": "BSD-3-Clause",
            "url": "https://asm.ow2.io/license.html"
          }
        }
      ]
    },
    {
      "type": "library",
      "group": "org.plumelib",
      "name": "hashmap-util",
      "version": "unspecified",
      "purl": "pkg:maven/org.plumelib/hashmap-util@unspecified",
      "externalReferences": [
        {
          "type": "website",
          "url": "https://github.com/plume-lib/hashmap-util"
        },
        {
          "type": "vcs",
          "url": "https://github.com/plume-lib/hashmap-util/"
        }
      ],
      "licenses": [
        {
          "license": {
            "name": "GPL-2.0 WITH Classpath-exception-2.0"
          }
        }
      ]
    },
    {
      "type": "library",
      "group": "org.plumelib",
      "name": "options",
      "version": "2.0.3",
      "purl": "pkg:maven/org.plumelib/options@2.0.3",
      "externalReferences": [
        {
          "type": "website",
          "url": "https://github.com/plume-lib/options"
        },
        {
          "type": "vcs",
          "url": "git@github.com:plume-lib/options.git"
        }
      ],
      "licenses": [
        {
          "license": {
            "name": "MIT License",
            "url": "https://opensource.org/licenses/MIT"
          }
        }
      ]
    },
    {
      "type": "library",
      "group": "org.plumelib",
      "name": "plume-util",
      "version": "1.12.2",
      "purl": "pkg:maven/org.plumelib/plume-util@1.12.2",
      "externalReferences": [
        {
          "type": "website",
          "url": "https://github.com/plume-lib/plume-util"
        },
        {
          "type": "vcs",
          "url": "https://github.com/plume-lib/plume-util/"
        }
      ],
      "licenses": [
        {
          "license": {
            "name": "MIT License",
            "url": "https://opensource.org/licenses/MIT"
          }
        }
      ]
    },
    {
      "type": "library",
      "group": "org.plumelib",
      "name": "reflection-util",
      "version": "unspecified",
      "purl": "pkg:maven/org.plumelib/reflection-util@unspecified",
      "externalReferences": [
        {
          "type": "website",
          "url": "https://github.com/plume-lib/reflection-util"
        },
        {
          "type": "vcs",
          "url": "https://github.com/plume-lib/reflection-util/"
        }
      ],
      "licenses": [
        {
          "license": {
            "name": "MIT License",
            "url": "https://opensource.org/licenses/MIT"
          }
        }
      ]
    }
  ]
}
```
