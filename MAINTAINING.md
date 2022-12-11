 
## Releasing mockk
 
 Main thing before release is to test it well. There was cases when after upgrade of library Android Instrumented was not tested and half a year was not usable.
 
 So checklist:
 
 - [ ] make sure all required PRs are merged
 - [ ] run test-suite in Gradle
 - [ ] run test-suite from Android Studio or IntelliJ from emulator (Android Instrumented tests)
 - [ ] release to local maven repo by running `gradle publish` with the `localrepo` repository uncommented in `mockk-publishing.gradle.kts`
 - [ ] do quick testing with this local maven repo: basics, if documentation is loading, if all dependencies are fine
 - [ ] change version to RELEASE version (i.e. remove -SNAPSHOT from version)
 - [ ] bump if needed major or minor (resetting everything afterwards)
 - [ ] commit it
 - [ ] tag it
 - [ ] redirect release process to oss.sonatype by uncommenting the sonatype repository in `mockk-publishing.gradle.kts`
 - [ ] release from Gradle with `gradle publish -Dorg.gradle.parallel=false` (apparently, sonatype does not like parallel builds)
 - [ ] goto oss.sonatype
 - [ ] find io.mockk repo
 - [ ] check state of dependencies (are sizes okay), maybe download one
 - [ ] close repo
 - [ ] release repo
 - [ ] wait till it appears on Maven central
 - [ ] bump version and append -SNAPSHOT
 - [ ] commit
 - [ ] push to GitHub
 - [ ] create GitHub release based on tag and describe changes there
 - [ ] announce on reddit/kotlinlang/potentially at "announcement place" on mockk.io
