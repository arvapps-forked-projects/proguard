/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
 */

package proguard

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.extensions.system.withSystemProperty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockkObject
import proguard.classfile.AccessConstants.PUBLIC
import testutils.asConfiguration
import java.io.ByteArrayOutputStream
import java.io.PrintStream

/**
 * Some simple testcases to catch special cases when parsing the Configuration.
 *
 * @author Thomas Neidhart
 */
class ConfigurationParserTest : FreeSpec({
    fun parseConfiguration(rules: String): Configuration {
        val configuration = Configuration()
        ConfigurationParser(rules, "", null, System.getProperties()).use {
            it.parse(configuration)
        }
        return configuration
    }

    fun parseConfiguration(reader: WordReader): Configuration {
        val configuration = Configuration()
        ConfigurationParser(reader, System.getProperties()).use {
            it.parse(configuration)
        }
        return configuration
    }

    fun parseRulesAsArguments(rules: String) = rules.split(' ', '\n').toTypedArray()

    "Keep rule tests" - {
        "Keep rule with <fields> wildcard should be valid" {
            parseConfiguration("-keep class * { <fields>; }")
        }

        "Keep rule with <fields> wildcard and public access modifier should be valid" {
            parseConfiguration("-keep class * { public <fields>; }")
        }

        "Keep rule with <fields> wildcard and public + protected access modifiers should be valid" {
            parseConfiguration("-keep class * { public protected <fields>; }")
        }

        "Keep rule with <methods> wildcard should be valid" {
            parseConfiguration("-keep class * { <methods>; }")
        }

        "Keep rule with <methods> wildcard and public access modifier should be valid" {
            parseConfiguration("-keep class * { public <methods>; }")
        }

        "Keep rule with <methods> wildcard and public + protected access modifier should be valid" {
            parseConfiguration("-keep class * { public protected <methods>; }")
        }

        "Keep rule with ClassName should be valid" {
            val configuration = parseConfiguration("-keep class ClassName { ClassName(); }")
            val keep = configuration.keep.single().methodSpecifications.single()
            keep.name shouldBe "<init>"
            keep.descriptor shouldBe "()V"
        }

        "Keep rule with ClassName and external class com.example.ClassName should be valid" {
            val configuration = parseConfiguration("-keep class com.example.ClassName { ClassName(); }")
            val keep = configuration.keep.single().methodSpecifications.single()
            keep.name shouldBe "<init>"
            keep.descriptor shouldBe "()V"
        }

        "Keep rule with <clinit> should be valid" {
            val configuration = parseConfiguration("-keep class ** { <clinit>(); }")
            val keep = configuration.keep.single().methodSpecifications.single()
            keep.name shouldBe "<clinit>"
            keep.descriptor shouldBe "()V"
        }

        "Keep rule with <clinit> and non-empty argument list should throw ParseException" {
            shouldThrow<ParseException> { parseConfiguration("-keep class * { void <clinit>(int) }") }
        }

        "Keep rule with * member wildcard and return type should be valid" {
            parseConfiguration("-keep class * { java.lang.String *; }")
        }

        "Keep rule with * member wildcard, return type and empty argument list should be valid" {
            parseConfiguration("-keep class * { int *(); }")
        }

        "Keep rule with * member wildcard, return type and non-empty argument list should be valid" {
            parseConfiguration("-keep class * { int *(int); }")
        }

        "Keep rule with <fields> wildcard and explicit type should throw ParseException" {
            shouldThrow<ParseException> { parseConfiguration("-keep class * { java.lang.String <fields>; }") }
        }

        "Keep rule with <methods> wildcard and explicit argument list should throw ParseException" {
            shouldThrow<ParseException> { parseConfiguration("-keep class * { <methods>(); }") }
        }
    }

    "A ParseException should be thrown with invalid annotation config at the end of the file" - {
        // This is a parse error without any further config after it.
        val configStr = ("-keep @MyAnnotation @ThisShouldBeInterfaceKeyword")

        "Then the option should throw a ParseException" {
            shouldThrow<ParseException> {
                configStr.asConfiguration()
            }
        }
    }

    "Testing -alwaysinline parsing" - {
        "Given an empty configuration" - {
            val savedPrintStream = System.out
            val customOutputStream = ByteArrayOutputStream()
            System.setOut(PrintStream(customOutputStream))

            parseConfiguration("")

            "The option does not print anything" {
                customOutputStream.toString() shouldContain ""
                System.setOut(savedPrintStream)
            }
        }

        "Given a configuration with -alwaysinline" - {
            val savedPrintStream = System.out
            val customOutputStream = ByteArrayOutputStream()
            System.setOut(PrintStream(customOutputStream))

            parseConfiguration(
                """-alwaysinline class * {
                        @org.chromium.build.annotations.AlwaysInline *;
                    }
                    """,
            )

            "The option prints out a warning" {
                customOutputStream.toString() shouldContain "Warning: The R8 option -alwaysinline is currently not " +
                    "supported by ProGuard.\nThis option will have no effect on the optimized artifact."
                System.setOut(savedPrintStream)
            }
        }

        "Given a configuration with -alwaysinline with no class specification" - {
            "The parsing should throw an exception" {
                shouldThrow<ParseException> { parseConfiguration("-alwaysinline") }
            }
        }
    }

    "Testing -identifiernamestring parsing" - {
        "Given an empty configuration" - {
            val savedPrintStream = System.out
            val customOutputStream = ByteArrayOutputStream()
            System.setOut(PrintStream(customOutputStream))

            parseConfiguration("")

            "The option does not print anything" {
                customOutputStream.toString() shouldContain ""
                System.setOut(savedPrintStream)
            }
        }

        "Given a configuration with -identifiernamestring" - {
            val savedPrintStream = System.out
            val customOutputStream = ByteArrayOutputStream()
            System.setOut(PrintStream(customOutputStream))

            parseConfiguration(
                """-identifiernamestring class * {
                        @org.chromium.build.annotations.IdentifierNameString *;
                    }
                    """,
            )

            "The option prints out a warning" {
                customOutputStream.toString() shouldContain "Warning: The R8 option -identifiernamestring is currently " +
                    "not supported by ProGuard.\nThis option will have no effect on the optimized artifact."
                System.setOut(savedPrintStream)
            }
        }

        "Given a configuration with -identifiernamestring with no class specification" - {
            "The parsing should throw an exception" {
                shouldThrow<ParseException> { parseConfiguration("-identifiernamestring") }
            }
        }
    }
    "Testing -maximumremovedandroidloglevel parsing" - {
        "Given an empty configuration" - {
            val savedPrintStream = System.out
            val customOutputStream = ByteArrayOutputStream()
            System.setOut(PrintStream(customOutputStream))

            parseConfiguration("")

            "The option does not print anything" {
                customOutputStream.toString() shouldContain ""
                System.setOut(savedPrintStream)
            }
        }

        "Given a configuration with -maximumremovedandroidloglevel without a class specification" - {
            val savedPrintStream = System.out
            val customOutputStream = ByteArrayOutputStream()
            System.setOut(PrintStream(customOutputStream))

            parseConfiguration("-maximumremovedandroidloglevel 1")

            "The option prints out a warning" {
                customOutputStream.toString() shouldContain "Warning: The R8 option -maximumremovedandroidloglevel is " +
                    "currently not supported by ProGuard.\nThis option will have no effect on the optimized artifact."
                System.setOut(savedPrintStream)
            }
        }

        "Given a configuration with -maximumremovedandroidloglevel with a class specification" - {
            val savedPrintStream = System.out
            val customOutputStream = ByteArrayOutputStream()
            System.setOut(PrintStream(customOutputStream))

            parseConfiguration(
                """
                -maximumremovedandroidloglevel 1 @org.chromium.build.annotations.DoNotStripLogs class ** {
                   <methods>;
                }
                """.trimIndent(),
            )

            "The option prints out a warning" {
                customOutputStream.toString() shouldContain "Warning: The R8 option -maximumremovedandroidloglevel is " +
                    "currently not supported by ProGuard.\nThis option will have no effect on the optimized artifact."
                System.setOut(savedPrintStream)
            }
        }
    }

    "Wildcard type tests" - {
        class TestConfig(
            val configOption: String,
            classSpecificationConfig: String,
            private val classSpecificationGetter: Configuration.() -> List<ClassSpecification>?,
        ) {
            private val configuration: Configuration by lazy {
                "$configOption $classSpecificationConfig".asConfiguration()
            }
            val classSpecifications: List<ClassSpecification>? get() = classSpecificationGetter.invoke(configuration)
        }

        fun generateTestCases(clSpec: String): List<TestConfig> =
            listOf(
                TestConfig("-keep", clSpec) { keep },
                TestConfig("-assumenosideeffects", clSpec) { assumeNoSideEffects },
                TestConfig("-assumenoexternalsideeffects", clSpec) { assumeNoExternalSideEffects },
                TestConfig("-assumenoescapingparameters", clSpec) { assumeNoEscapingParameters },
                TestConfig("-assumenoexternalreturnvalues", clSpec) { assumeNoExternalReturnValues },
                TestConfig("-assumevalues", clSpec) { assumeValues },
            )

        "Test wildcard matches all methods and fields" {
            val testConfigurations = generateTestCases("class Foo { *; }") + generateTestCases("class Foo { <fields>; <methods>; }")

            for (testConfig in testConfigurations) {
                val classSpecifications = testConfig.classSpecifications
                val methodSpecification = classSpecifications?.single()?.methodSpecifications?.single()
                methodSpecification shouldNotBe null
                methodSpecification?.requiredSetAccessFlags shouldBe 0
                methodSpecification?.name shouldBe null
                methodSpecification?.descriptor shouldBe null
                val fieldSpecification = classSpecifications?.single()?.fieldSpecifications?.single()
                fieldSpecification shouldNotBe null
                fieldSpecification?.requiredSetAccessFlags shouldBe 0
                fieldSpecification?.name shouldBe null
                fieldSpecification?.descriptor shouldBe null
            }
        }

        "Test wildcard method return type" {
            val testConfigurations = generateTestCases("class Foo { * bar(); }")

            for (testConfig in testConfigurations) {
                val classSpecifications = testConfig.classSpecifications
                val methodSpecification = classSpecifications?.single()?.methodSpecifications?.single()
                methodSpecification?.requiredSetAccessFlags shouldBe 0
                methodSpecification?.name shouldBe "bar"
                methodSpecification?.descriptor shouldBe "()L*;"
                val fieldSpecification = classSpecifications?.single()?.fieldSpecifications
                fieldSpecification shouldBe null
            }
        }

        "Test wildcard method return type with access modifier" {
            val testConfigurations = generateTestCases("class Foo { public * bar(); }")

            for (testConfig in testConfigurations) {
                val classSpecifications = testConfig.classSpecifications
                val methodSpecification = classSpecifications?.single()?.methodSpecifications?.single()
                methodSpecification?.requiredSetAccessFlags shouldBe PUBLIC
                methodSpecification?.name shouldBe "bar"
                methodSpecification?.descriptor shouldBe "()L*;"
                val fieldSpecification = classSpecifications?.single()?.fieldSpecifications
                fieldSpecification shouldBe null
            }
        }

        "Test wildcard field type" {
            val testConfigurations = generateTestCases("class Foo { * bar; }")

            for (testConfig in testConfigurations) {
                val classSpecifications = testConfig.classSpecifications
                val methodSpecification = classSpecifications?.single()?.methodSpecifications
                methodSpecification shouldBe null
                val fieldSpecification = classSpecifications?.single()?.fieldSpecifications?.single()
                fieldSpecification?.requiredSetAccessFlags shouldBe 0
                fieldSpecification?.name shouldBe "bar"
                fieldSpecification?.descriptor shouldBe "L*;"
            }
        }

        "Test wildcard field type with access modifier" {
            val testConfigurations = generateTestCases("class Foo { public * bar; }")

            for (testConfig in testConfigurations) {
                val classSpecifications = testConfig.classSpecifications
                val methodSpecification = classSpecifications?.single()?.methodSpecifications
                methodSpecification shouldBe null
                val fieldSpecification = classSpecifications?.single()?.fieldSpecifications?.single()
                fieldSpecification?.requiredSetAccessFlags shouldBe PUBLIC
                fieldSpecification?.name shouldBe "bar"
                fieldSpecification?.descriptor shouldBe "L*;"
            }
        }

        "Test all type wildcard field" {
            val testConfigurations = generateTestCases("class Foo { *** bar; }")

            for (testConfig in testConfigurations) {
                val classSpecifications = testConfig.classSpecifications
                val methodSpecification = classSpecifications?.single()?.methodSpecifications
                methodSpecification shouldBe null
                val fieldSpecification = classSpecifications?.single()?.fieldSpecifications?.single()
                fieldSpecification?.requiredSetAccessFlags shouldBe 0
                fieldSpecification?.name shouldBe "bar"
                fieldSpecification?.descriptor shouldBe "L***;"
            }
        }

        "Test all type wildcard field type with access modifier" {
            val testConfigurations = generateTestCases("class Foo { public *** bar; }")

            for (testConfig in testConfigurations) {
                val classSpecifications = testConfig.classSpecifications
                val methodSpecification = classSpecifications?.single()?.methodSpecifications
                methodSpecification shouldBe null
                val fieldSpecification = classSpecifications?.single()?.fieldSpecifications?.single()
                fieldSpecification?.requiredSetAccessFlags shouldBe PUBLIC
                fieldSpecification?.name shouldBe "bar"
                fieldSpecification?.descriptor shouldBe "L***;"
            }
        }

        "Test all type wildcard method return type" {
            val testConfigurations = generateTestCases("class Foo { *** bar(); }")

            for (testConfig in testConfigurations) {
                val classSpecifications = testConfig.classSpecifications
                val methodSpecification = classSpecifications?.single()?.methodSpecifications?.single()
                methodSpecification?.requiredSetAccessFlags shouldBe 0
                methodSpecification?.name shouldBe "bar"
                methodSpecification?.descriptor shouldBe "()L***;"
                val fieldSpecification = classSpecifications?.single()?.fieldSpecifications
                fieldSpecification shouldBe null
            }
        }

        "Test all type wildcard method return type with access modifier" {
            val testConfigurations = generateTestCases("class Foo { public *** bar(); }")

            for (testConfig in testConfigurations) {
                val classSpecifications = testConfig.classSpecifications
                val methodSpecification = classSpecifications?.single()?.methodSpecifications?.single()
                methodSpecification?.requiredSetAccessFlags shouldBe PUBLIC
                methodSpecification?.name shouldBe "bar"
                methodSpecification?.descriptor shouldBe "()L***;"
                val fieldSpecification = classSpecifications?.single()?.fieldSpecifications
                fieldSpecification shouldBe null
            }
        }

        "Test concrete wildcard field type" {
            val testConfigurations = generateTestCases("class Foo { java.lang.String bar; }")

            for (testConfig in testConfigurations) {
                val classSpecifications = testConfig.classSpecifications
                val methodSpecification = classSpecifications?.single()?.methodSpecifications
                methodSpecification shouldBe null
                val fieldSpecification = classSpecifications?.single()?.fieldSpecifications?.single()
                fieldSpecification?.requiredSetAccessFlags shouldBe 0
                fieldSpecification?.name shouldBe "bar"
                fieldSpecification?.descriptor shouldBe "Ljava/lang/String;"
            }
        }

        "Test concrete wildcard method return type" {
            val testConfigurations = generateTestCases("class Foo { java.lang.String bar(); }")

            for (testConfig in testConfigurations) {
                val classSpecifications = testConfig.classSpecifications
                val methodSpecification = classSpecifications?.single()?.methodSpecifications?.single()
                methodSpecification?.requiredSetAccessFlags shouldBe 0
                methodSpecification?.name shouldBe "bar"
                methodSpecification?.descriptor shouldBe "()Ljava/lang/String;"
                val fieldSpecification = classSpecifications?.single()?.fieldSpecifications
                fieldSpecification shouldBe null
            }
        }
    }

    "Class specification with unicode identifiers" - {
        "Given some -keep rules with class specifications containing supported characters for DEX file" - {
            val rules =
                """
                -keep class uu.☱ { *; }
                -keep class uu.o { ** ☱; }
                -keep class uu.o { *** ☱(); }
                -keep class uu.o1
                -keep class uu.o${"$"}o
                -keep class uu.o-o
                -keep class uu.o_o
                -keep class ‐ { <methods>; }
                -keep class ‧ { <fields>; }
                -keep class ‰
                -keep class * { ** ퟿(...); }
                -keep class { *; }
                -keep class ￯ { int[] foo; }
                -if class **𐀀 { ** 𐀀*(); } 
                -keep class <1> { ** 𐀀*(); }
                -keep class * extends 􏿿
                -keep class ** implements ☱
                """.trimIndent()

            val reader = ArgumentWordReader(parseRulesAsArguments(rules), null)
            mockkObject(reader)
            every { reader.locationDescription() } returns "dummyOrigin"

            "When the given rules are parsed" - {
                withSystemProperty("proguard.use.dalvik.identifier.verification", "true") {
                    val configuration = parseConfiguration(reader)
                    "Then 'keep' should contain 16 keep class specifications" {
                        configuration.keep shouldHaveSize 16
                    }
                }
            }
        }

        "Given some -keep rules with class specifications containing unsupported identifier for DEX file" - {
            val rules =
                """
                -keep class uu.${String(Character.toChars(0x00a1 - 1))} { *; }
                -keep class uu.o { ** ${String(Character.toChars(0x1fff + 1))}; }
                -keep class uu.o { *** ${String(Character.toChars(0x2010 - 1))}(); }
                -keep class ${String(Character.toChars(0x2027 + 1))} { <methods>; }
                -keep class ${String(Character.toChars(0x2030 - 1))} { <fields>; }
                -keep class ${String(Character.toChars(0xd7ff + 1))}
                -keep class * { ** ${String(Character.toChars(0xe000 - 1))}(...); }
                -keep class ${String(Character.toChars(0xffef + 1))} { *; }
                -keep class ${String(Character.toChars(0x10000 - 1))} { int[] foo; }
                -keep class * extends !
                -keep class ** implements @
                -keep class #
                -keep class %
                -keep class ^
                -keep class &
                -keep class ;
                -keep class ,
                """.trimIndent()

            val reader = ArgumentWordReader(parseRulesAsArguments(rules), null)
            mockkObject(reader)
            every { reader.locationDescription() } returns "dummyOrigin"

            "When the given rule is parsed, then a ParseException should be thrown" - {
                withSystemProperty("proguard.use.dalvik.identifier.verification", "true") {
                    shouldThrow<ParseException> { parseConfiguration(reader) }
                }
            }
        }
    }
})
