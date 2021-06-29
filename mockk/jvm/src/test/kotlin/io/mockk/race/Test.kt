package io.mockk.race

import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    StaticMockStateATest::class,
    StaticMockStateBTest::class
)
class FeatureTestSuite
