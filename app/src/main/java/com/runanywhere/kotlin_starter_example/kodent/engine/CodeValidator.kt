package com.runanywhere.kotlin_starter_example.kodent.engine

object CodeValidator {

    fun looksLikeCode(code: String): Boolean {
        val trimmed = code.trim()
        if (trimmed.length < 5) return false

        val commonSignals = listOf(
            "{", "}", "(", ")", ";", "=", "[", "]", "->", "=>",
            "fun ", "def ", "function ", "class ", "void ", "int ",
            "var ", "val ", "let ", "const ",
            "if ", "else", "for ", "while ", "return",
            "import ", "package ", "#include",
            "print", "console.log", "echo ",
            "public ", "private ", "static "
        )

        val signalCount = commonSignals.count {
            trimmed.contains(it, ignoreCase = false)
        }

        return signalCount >= 2
    }
}