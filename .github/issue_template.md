# Please remove below sections wisely

Below information is actually needed to make all the process of fixing faster.
Choose main points, don't blindly follow this as a set of rules. 
So don't waste much time, usually main thing is to have good reproducible minimal code.

# Prerequisites

Please answer the following questions for yourself before submitting an issue.

- [ ] I am running the latest version
- [ ] I checked the documentation and found no answer
- [ ] I checked to make sure that this issue has not already been filed
- [ ] I'm reporting the issue to the correct repository (for multi-repository projects)

# Expected Behavior

Please describe the behavior you are expecting

# Current Behavior

What is the current behavior?

# Failure Information (for bugs)

Please help provide information about the failure if this is a bug. If it is not a bug, please remove the rest of this template.

## Steps to Reproduce

Please provide detailed steps for reproducing the issue.

1. step 1
2. step 2
3. you get it...

## Context

Please provide any relevant information about your setup. This is important in case the issue is not reproducible except for under certain conditions.

* OS:
* JDK version:
* Type of test: unit test OR android instrumentation test


## Failure Logs

Please include any relevant log snippets or files here.

## Minimal reproducible code (gist of this issue)

```kotlin
// -----------------------[ GRADLE DEFINITIONS ] -----------------------
dependencies {
    testCompile group: 'org.apache.kafka', name: 'kafka_2.12', version: '1.1.0'
}
// -----------------------[ YOUR CODE STARTS HERE ] -----------------------
package io.mockk.gh

import io.mockk.every
import io.mockk.mockk
import org.apache.kafka.common.KafkaFuture
import kotlin.test.Test

class Issue69Test {

    @Test
    fun test() {
        val kafkaFuture: KafkaFuture<Void> = mockk()
        every { kafkaFuture.get(any(), any()) } returns mockk()
        kafkaFuture.get(10, TimeUnit.MILLISECONDS)
    }

}
// -----------------------[ YOUR CODE ENDS HERE ] -----------------------
```
