package io.mockk.test

/**
 * Class file source for [Issue1451ModifierInterferenceTest]: the test redefines this class from its
 * original bytes with the final modifier stripped, so it must stay a top-level class (no
 * InnerClasses attribute) and must not be loaded anywhere else.
 */
class Issue1451ModifierInterferenceAlteredFinal {
    fun value(): Int = 1
}
