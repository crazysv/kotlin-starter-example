package com.runanywhere.kotlin_starter_example.kodent.engine

object HealthFixSnippets {

    val FORCE_UNWRAP = """
        |// Instead of:
        |val name = user!!.name
        |
        |// Use safe call:
        |val name = user?.name ?: "default"
        |
        |// Or let block:
        |user?.let { u ->
        |    println(u.name)
        |}
    """.trimMargin()

    val RESOURCE_LEAK = """
        |// Instead of:
        |val stream = FileInputStream("file.txt")
        |val data = stream.read()
        |stream.close() // might not reach here!
        |
        |// Use .use {} block:
        |FileInputStream("file.txt").use { stream ->
        |    val data = stream.read()
        |} // auto-closes even on exception
    """.trimMargin()

    val STRING_CONCAT_LOOP = """
        |// Instead of:
        |var result = ""
        |for (item in list) {
        |    result += item.toString() // O(n²)!
        |}
        |
        |// Use buildString:
        |val result = buildString {
        |    for (item in list) {
        |        append(item)
        |    }
        |}
        |
        |// Or joinToString:
        |val result = list.joinToString("")
    """.trimMargin()

    val GLOBAL_SCOPE = """
        |// Instead of:
        |GlobalScope.launch { ... } // Leaks!
        |
        |// In ViewModel:
        |viewModelScope.launch { ... }
        |
        |// In Activity/Fragment:
        |lifecycleScope.launch { ... }
        |
        |// In Composable:
        |LaunchedEffect(key) { ... }
    """.trimMargin()

    val THREAD_SLEEP = """
        |// Instead of:
        |Thread.sleep(1000) // Blocks thread!
        |
        |// Use coroutine delay:
        |delay(1000) // Suspends, doesn't block
    """.trimMargin()

    val SEQUENCE = """
        |// Instead of:
        |list.filter { it > 5 }.map { it * 2 }
        |// Creates 2 intermediate lists!
        |
        |// Use sequence:
        |list.asSequence()
        |    .filter { it > 5 }
        |    .map { it * 2 }
        |    .toList() // Only 1 list created
    """.trimMargin()

    val IO_CONTEXT = """
        |// Instead of:
        |fun loadData() {
        |    val data = File("data.txt").readText()
        |    // Blocks main thread!
        |}
        |
        |// Use IO dispatcher:
        |suspend fun loadData() = withContext(Dispatchers.IO) {
        |    File("data.txt").readText()
        |}
    """.trimMargin()

    val REMEMBER_STATE = """
        |// Instead of:
        |@Composable
        |fun MyScreen() {
        |    var count = mutableStateOf(0)
        |    // Resets on recomposition!
        |}
        |
        |// Use remember:
        |@Composable
        |fun MyScreen() {
        |    var count by remember { mutableStateOf(0) }
        |}
    """.trimMargin()

    val NULL_CHECK_IDIOMATIC = """
        |// Instead of:
        |if (user != null) {
        |    println(user.name)
        |}
        |
        |// Use let:
        |user?.let { println(it.name) }
        |
        |// Or elvis for default:
        |val name = user?.name ?: "Unknown"
    """.trimMargin()

    val DATA_CLASS = """
        |// Instead of:
        |class User(val name: String, val age: Int)
        |
        |// Use data class (auto equals/hashCode/copy):
        |data class User(val name: String, val age: Int)
    """.trimMargin()

    val INDEX_LOOP = """
        |// Instead of:
        |for (i in 0 until list.size) {
        |    println(list[i])
        |}
        |
        |// Use forEach:
        |list.forEach { println(it) }
        |
        |// If you need index:
        |list.forEachIndexed { i, item ->
        |    println("${'$'}i: ${'$'}item")
        |}
    """.trimMargin()

    val DEPRECATED_HANDLER = """
        |// Instead of:
        |Handler().postDelayed({ }, 1000)
        |
        |// Use Looper:
        |Handler(Looper.getMainLooper()).postDelayed({ }, 1000)
        |
        |// Or better, use coroutines:
        |lifecycleScope.launch {
        |    delay(1000)
        |    // do work
        |}
    """.trimMargin()

    val VIEW_BINDING = """
        |// Instead of:
        |val tv = findViewById<TextView>(R.id.title)
        |
        |// Use ViewBinding:
        |// build.gradle: viewBinding { enabled = true }
        |private lateinit var binding: ActivityMainBinding
        |
        |override fun onCreate(savedInstanceState: Bundle?) {
        |    binding = ActivityMainBinding.inflate(layoutInflater)
        |    setContentView(binding.root)
        |    binding.title.text = "Hello"
        |}
    """.trimMargin()

    val RECYCLER_DIFF = """
        |// Instead of:
        |adapter.notifyDataSetChanged() // Redraws all!
        |
        |// Use ListAdapter with DiffUtil:
        |class MyAdapter : ListAdapter<Item, VH>(Diff()) {
        |    class Diff : DiffUtil.ItemCallback<Item>() {
        |        override fun areItemsTheSame(a: Item, b: Item) =
        |            a.id == b.id
        |        override fun areContentsTheSame(a: Item, b: Item) =
        |            a == b
        |    }
        |}
        |// Then: adapter.submitList(newList)
    """.trimMargin()

    val UNSAFE_CAST = """
        |// Instead of:
        |val user = obj as User // Crashes if wrong type!
        |
        |// Use safe cast:
        |val user = obj as? User ?: return
        |
        |// Or with when:
        |when (obj) {
        |    is User -> handleUser(obj)
        |    is Admin -> handleAdmin(obj)
        |    else -> handleUnknown()
        |}
    """.trimMargin()

    val EMPTY_CATCH = """
        |// Instead of:
        |try { riskyOp() } catch (e: Exception) { }
        |
        |// Log the error:
        |try {
        |    riskyOp()
        |} catch (e: IOException) {
        |    Log.e(TAG, "Failed to do X", e)
        |} catch (e: Exception) {
        |    Log.e(TAG, "Unexpected error", e)
        |}
    """.trimMargin()

    val LATEINIT_CHECK = """
        |// Instead of:
        |lateinit var data: String
        |fun use() { println(data) } // Crash if not set!
        |
        |// Check before use:
        |if (::data.isInitialized) {
        |    println(data)
        |}
        |
        |// Or use lazy instead:
        |val data: String by lazy { loadData() }
    """.trimMargin()

    val THREAD_SAFETY = """
        |// Instead of:
        |var count = 0 // Not thread-safe!
        |launch { count++ }
        |launch { count++ }
        |
        |// Use atomic:
        |val count = AtomicInteger(0)
        |launch { count.incrementAndGet() }
        |
        |// Or Mutex:
        |val mutex = Mutex()
        |launch { mutex.withLock { count++ } }
    """.trimMargin()

    val SIDE_EFFECT = """
        |// Instead of:
        |@Composable
        |fun MyScreen() {
        |    scope.launch { loadData() } // Runs every recomposition!
        |}
        |
        |// Use LaunchedEffect:
        |@Composable
        |fun MyScreen() {
        |    LaunchedEffect(Unit) {
        |        loadData() // Runs once
        |    }
        |}
    """.trimMargin()

    val CONTEXT_LEAK = """
        |// Instead of:
        |companion object {
        |    var context: Context? = null // Leaks Activity!
        |}
        |
        |// Use application context:
        |companion object {
        |    lateinit var appContext: Context
        |}
        |// Set in Application.onCreate():
        |appContext = applicationContext
    """.trimMargin()

    val DIVISION_ZERO = """
        |// Instead of:
        |val avg = total / count // Crash if count == 0!
        |
        |// Add check:
        |val avg = if (count > 0) total / count else 0
        |
        |// Or use takeIf:
        |val avg = count.takeIf { it > 0 }
        |    ?.let { total / it } ?: 0
    """.trimMargin()

    val BOUNDS_CHECK = """
        |// Instead of:
        |val item = list[index] // Crash if out of bounds!
        |
        |// Use getOrNull:
        |val item = list.getOrNull(index)
        |
        |// Or getOrElse:
        |val item = list.getOrElse(index) { defaultValue }
        |
        |// Or check first:
        |if (index in list.indices) { list[index] }
    """.trimMargin()

    val STRING_TEMPLATE = """
        |// Instead of:
        |"Hello " + name + "! Age: " + age
        |
        |// Use string templates:
        |"Hello ${'$'}name! Age: ${'$'}age"
        |
        |// For expressions:
        |"Total: ${'$'}{items.size} items"
    """.trimMargin()

    val BITMAP_SAMPLING = """
        |// Instead of:
        |val bitmap = BitmapFactory.decodeFile(path) // OOM!
        |
        |// Use sampling:
        |val options = BitmapFactory.Options().apply {
        |    inJustDecodeBounds = true
        |}
        |BitmapFactory.decodeFile(path, options)
        |options.inSampleSize = calculateInSampleSize(
        |    options, reqWidth, reqHeight
        |)
        |options.inJustDecodeBounds = false
        |val bitmap = BitmapFactory.decodeFile(path, options)
    """.trimMargin()

    fun getFixFor(issueTitle: String): String? {
        val lower = issueTitle.lowercase()
        return when {
            lower.contains("force unwrap") || lower.contains("!!") -> FORCE_UNWRAP
            lower.contains("resource leak") || lower.contains("unclosed") -> RESOURCE_LEAK
            lower.contains("string concatenation") && lower.contains("loop") -> STRING_CONCAT_LOOP
            lower.contains("globalscope") -> GLOBAL_SCOPE
            lower.contains("thread.sleep") -> THREAD_SLEEP
            lower.contains("intermediate") || lower.contains("assequence") || lower.contains("collection") -> SEQUENCE
            lower.contains("i/o") || lower.contains("background thread") || lower.contains("without background") -> IO_CONTEXT
            lower.contains("remember") || lower.contains("state without") -> REMEMBER_STATE
            lower.contains("non-idiomatic null") || lower.contains("null check") -> NULL_CHECK_IDIOMATIC
            lower.contains("data class") || lower.contains("consider data") -> DATA_CLASS
            lower.contains("index loop") || lower.contains("foreach") || lower.contains("index instead") -> INDEX_LOOP
            lower.contains("handler") || lower.contains("deprecated") && lower.contains("handler") -> DEPRECATED_HANDLER
            lower.contains("viewbinding") || lower.contains("findviewbyid") -> VIEW_BINDING
            lower.contains("notifydatasetchanged") || lower.contains("diffutil") -> RECYCLER_DIFF
            lower.contains("unsafe cast") -> UNSAFE_CAST
            lower.contains("empty catch") -> EMPTY_CATCH
            lower.contains("lateinit") -> LATEINIT_CHECK
            lower.contains("thread safety") || lower.contains("synchroniz") -> THREAD_SAFETY
            lower.contains("side effect") -> SIDE_EFFECT
            lower.contains("context leak") -> CONTEXT_LEAK
            lower.contains("division") || lower.contains("divide") -> DIVISION_ZERO
            lower.contains("index") && lower.contains("bounds") -> BOUNDS_CHECK
            lower.contains("string concat") && !lower.contains("loop") -> STRING_TEMPLATE
            lower.contains("bitmap") || lower.contains("sampling") -> BITMAP_SAMPLING
            lower.contains("raw thread") -> GLOBAL_SCOPE
            lower.contains("long function") || lower.contains("very long") -> null // No code fix for this
            lower.contains("magic number") -> null
            lower.contains("poor") || lower.contains("single-letter") -> null
            lower.contains("comment") || lower.contains("lack of") -> null
            lower.contains("mutable") || lower.contains("var") -> null
            else -> null
        }
    }
}