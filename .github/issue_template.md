## Please remove sections wisely

Below information is actually needed to make all the process of fixing faster.
Choose main points. Don't blindly follow this as a set of rules. 
Don't waste much time. Usually, the main thing is to have a good reproducible minimal code.

### Prerequisites

Please answer the following questions for yourself before submitting an issue.

- [ ] I am running the latest version
- [ ] I checked the documentation and found no answer
- [ ] I checked to make sure that this issue has not already been filed

### Expected Behavior

Please describe the behavior you are expecting

### Current Behavior

What is the current behavior?

### Failure Information (for bugs)

Please help provide information about the failure if this is a bug. If it is not a bug, please remove the rest of this template.

#### Steps to Reproduce

Please provide detailed steps for reproducing the issue.

1. step 1
2. step 2
3. you get it...

#### Context

Please provide any relevant information about your setup. This is important in case the issue is not reproducible except for under certain conditions.

* MockK version:
* OS:
* Kotlin version:
* JDK version:
* Type of test: unit test OR android instrumented test


#### Failure Logs

Please include any relevant log snippets or files here.

#### Stack trace

```
// -----------------------[ YOUR STACK STARTS HERE ] -----------------------
io.mockk.MockKException: Class cast exception. Probably type information was erased.
In this case use `hint` before call to specify exact return type of a method. 

	at io.mockk.impl.InternalPlatform.prettifyRecordingException(InternalPlatform.kt:83)
	at io.mockk.impl.eval.RecordedBlockEvaluator.record(RecordedBlockEvaluator.kt:50)
	at io.mockk.impl.eval.EveryBlockEvaluator.every(EveryBlockEvaluator.kt:25)
	at eu.example.schedule.kafka.KafkaAdminImplTest.mockkVoidReturnFails(KafkaAdminImplTest.kt:301)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:50)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:47)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
	at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:78)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:57)
	at org.junit.runners.ParentRunner$3.run(ParentRunner.java:290)
	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)
	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
	at org.junit.runners.ParentRunner.access$000(ParentRunner.java:58)
	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:268)
	at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
	at org.junit.runner.JUnitCore.run(JUnitCore.java:137)
	at com.intellij.junit4.JUnit4IdeaTestRunner.startRunnerWithArgs(JUnit4IdeaTestRunner.java:68)
	at com.intellij.rt.execution.junit.IdeaTestRunner$Repeater.startRunnerWithArgs(IdeaTestRunner.java:47)
	at com.intellij.rt.execution.junit.JUnitStarter.prepareStreamsAndStart(JUnitStarter.java:242)
	at com.intellij.rt.execution.junit.JUnitStarter.main(JUnitStarter.java:70)
Caused by: java.lang.ClassCastException: kotlin.Unit cannot be cast to java.lang.Void
	at eu.example.schedule.kafka.KafkaAdminImplTest$mockkVoidReturnFails$1.invoke(KafkaAdminImplTest.kt:87)
	at eu.example.schedule.kafka.KafkaAdminImplTest$mockkVoidReturnFails$1.invoke(KafkaAdminImplTest.kt:24)
	at io.mockk.impl.eval.RecordedBlockEvaluator$record$block$1.invoke(RecordedBlockEvaluator.kt:22)
	at io.mockk.impl.recording.JvmAutoHinter.autoHint(JvmAutoHinter.kt:23)
	at io.mockk.impl.eval.RecordedBlockEvaluator.record(RecordedBlockEvaluator.kt:31)
	... 25 more
// -----------------------[ YOUR STACK TRACE ENDS HERE ] -----------------------
```

#### Minimal reproducible code (the gist of this issue)

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
