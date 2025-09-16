package com.example.todoapp

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isGone
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.todoapp.databinding.ActivityMainBinding
import java.util.Date
import androidx.activity.viewModels
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

private const val TO_DO_ITEM_PREFERENCES_DATASTORE_NAME = "to_do_item_preferences"

private val Context.dataStore by preferencesDataStore(
    name = TO_DO_ITEM_PREFERENCES_DATASTORE_NAME
)

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

        //ViewModel сохраняет информацию о состоянии UI при пересоздании activity
    private val viewModel :ToDoItemViewModel  by viewModels()
        //adapter для recyclerView
    private lateinit var mainAdapter: ToDoItemRecyclerAdapter

    private lateinit var toDoItemRepository: ToDoItemRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if(!viewModel.isAdaptRepInitialize){
            viewModel.toDoItemRepository = ToDoItemRepository(applicationContext.dataStore, lifecycleScope, this)
            viewModel.toDoItemRepository.nextId()
            viewModel.toDoItemRecyclerAdapter = ToDoItemRecyclerAdapter(viewModel.toDoItemRepository)
            viewModel.toDoItemRecyclerAdapter.sortOrder = Order.CreateDateCheckPriority
            viewModel.isAdaptRepInitialize = true
        }  
        toDoItemRepository = viewModel.toDoItemRepository
        mainAdapter = viewModel.toDoItemRecyclerAdapter

        lifecycle.addObserver(mainAdapter)

        init()

        Toast.makeText(toDoItemRepository.context, "1212", Toast.LENGTH_SHORT).show()
            //работа ViewModel
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.uiState.collect{
                    findViewById<ConstraintLayout>(R.id.constraintLayoutEdit).isGone = !it.isTextEditVisible
                }
            }
        }
        lifecycleScope.launch {
            /*val deferredCount = toDoItemRepository.lifecycleScope.async {
                var ret = 0
                toDoItemRepository.toDoItemRepositoryFlow.take(1).collect(){
                    /*var position: Int         //test getPositionPerToDoItem == getToDoItemPerPosition
                    var toDoId: Int
                    var f: String
                    for(or in Order.entries){
                        for(n in it[0]){
                            toDoId = n.key
                            position = toDoItemRepository.getPositionPerToDoItem(or, toDoId)!!
                            if(toDoId != toDoItemRepository.getToDoItemPerPosition(or, position).id)
                                throw Exception("getPositionPerToDoItem != getToDoItemPerPosition")
                        }
                    }*/
                    ret = it[1].size
                }
                ret
            }
            mainAdapter.itemCount = deferredCount.await()
            mainAdapter.notifyItemRangeInserted(0, mainAdapter.itemCount)*/

            repeatOnLifecycle(Lifecycle.State.STARTED) {
                toDoItemRepository.toDoItemRepositoryFlow.collect(){ // it - deletedPositions, newPositions, oldChangePositions, newChangePositions
                    val deletedPositions: ArrayList<Int> = it[0] //позиции удалённых элементов
                    val newPositions: ArrayList<Int> = it[1] // позиции добавленых элементов
                    val oldChangePositions: ArrayList<Int> = it[2] // позиции откуда элементы переместили
                    val newChangePositions: ArrayList<Int> = it[3] // позиции куда элементы переместили
                    //оповещаем адаптер об удалении
                    deletedPositions.sort()
                    for(n in deletedPositions.size-1 downTo 0){
                        mainAdapter.itemCount--
                        mainAdapter.notifyItemRemoved(n)
                    }
                    //опповещаем адаптер об добавлении
                    newPositions.sort()
                    for(n in newPositions) {
                        mainAdapter.itemCount++
                        mainAdapter.notifyItemInserted(n)
                    }
                    //проверяем, что количества позиций куда и откуда перемащать элементы совпадают
                    if(oldChangePositions.size!=newChangePositions.size)
                        throw Exception("oldChangePositions.size != newChangePositions.size")

                    fun binarySearch(array: ArrayList<Int>, searchElement: Int): Int{
                        var l = -1
                        var r = array.size - 1
                        var m = 0
                        while (l != r){
                            m = (l+r+1)/2
                            if(array[m] <= searchElement)
                                l = m
                            else
                                r = m - 1
                        }
                        return  l
                    }
                    //оповещаем адаптер об перемещении
                    for(n in 0..< oldChangePositions.size){
                        var oldPosition = oldChangePositions[n] //позиция откуда элемент перемистился
                        val newPosition = newChangePositions[n] //позиция куда элемент перемистился
                        val lessDelElementCount = binarySearch(deletedPositions, oldPosition) + 1 //количество удалённых позиций, меньше (<=) текущего элемента
                        val lessNewElementCount = binarySearch(newPositions, oldPosition) + 1 //количество добавленных позиций, меньше (<=) текущего элемента

                        deletedPositions.add(lessDelElementCount, oldPosition) //добавляем старую позицию в удалённые, так как эелемент будет перемещён (всё равно, что удалён)
                        newPositions.add(binarySearch(newPositions, newPosition) + 1, newPosition) //добавляем новую позицию в добавленные, так как эелемент будет перемещён (всё равно, что добавлен)
                        // вычитаем удалённые и прибавляем новые, чтобы найти настоящую позицию (изменивщуюся из-за удаления и добавления элементов)
                        oldPosition += (lessNewElementCount - lessDelElementCount)

                        mainAdapter.notifyItemMoved(oldPosition, newPosition)
                        mainAdapter.notifyItemChanged(newPosition)
                    }
                }
            }
        }/*
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                toDoItemRepository.toDoItemRepositoryFlow.collect() {
                    mainAdapter.itemCount = it
                }
            }
        }*/

        /*lifecycleScope.launch {
            for(toDoItem in toDoItemRepository.getAllToDoItems()) {
                mainAdapter.add(toDoItem)
            }
        }*/

        ////mainAdapter.add()
        //mainAdapter.add(ToDoItem(2, "SuperHotSuperHotSuperHotSuperHotSuperHotSuperHot", Date()))

    }

    private fun init(){ binding.apply {
                //выбор режима recyclerView
            recyclerView.layoutManager = LinearLayoutManager(this@MainActivity)
                //назначение адаптера для recyclerView
            recyclerView.adapter = mainAdapter
                //adapter для сппинера
            val adapterSpinner = ArrayAdapter.createFromResource(this@MainActivity, R.array.spinnerPriority, android.R.layout.simple_spinner_item)
                //назначение адаптера для спиннера
            spinnerPriority.adapter = adapterSpinner
//---ClickListeners-->//
                //нажатие кнопки для добавления дела
            buttonAdd.setOnClickListener{
                openEditText()
            }
                //очистка поля ввода
            buttonClear.setOnClickListener{
                editTextMultiLine.editableText.clear()
            }
                //нажатие кнопки назад
            buttonBack.setOnClickListener{
                closeEditText(it)
            }
                //нажатие кнопки подтверждения
            buttonOk.setOnClickListener{ if(editTextMultiLine.text.toString().trim() !=""){
                createTDItem()
                closeEditText(it)
                editTextMultiLine.editableText.clear()
                spinnerPriority.setSelection(1)
            }}
                //изменение цвета при выборе приоритета дела
            spinnerPriority.onItemSelectedListener = object :
                AdapterView.OnItemSelectedListener{
                    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                        R.color.systemNotGreen = when(spinnerPriority.selectedItemId){
                            0L -> R.color.lowPriority
                            1L -> R.color.usualPriority
                            2L -> R.color.emergencyPriority
                            else -> R.color.usualPriority
                        }
                        reColorEditText()
                    }

                    override fun onNothingSelected(p0: AdapterView<*>?) {
                        spinnerPriority.setSelection(0)
                    }
                }
//<--ClickListeners---//
            //spinnerPriority.setSelection(1)



    }}

        //открытие редактора дела
    private fun openEditText(){ binding.apply {
            buttonAdd.isGone = true
            constraintLayoutEdit.isGone = false
            viewModel.setTextEditVisible(true)
    }}
        //создание нового дела
    private fun createTDItem(){ binding.apply {
            val toDoItem = ToDoItem(toDoItemRepository.nextId(), editTextMultiLine.text.toString(), Date(), Priority.entries[spinnerPriority.selectedItemId.toInt()])
            toDoItemRepository.addToDoItem(toDoItem)
    }}
        //закрытие редактора дела
    private fun closeEditText(it: View){ binding.apply {
            constraintLayoutEdit.isGone = true
            buttonAdd.isGone = false
            viewModel.setTextEditVisible(false)
            closeKeyboard(it)
    }}
        //обновление цвета меню
    private fun reColorEditText() { binding.apply {
        spinnerPriority.setBackgroundResource(R.color.systemNotGreen)
        textViewPriority.setBackgroundResource(R.color.systemNotGreen)

    }}
        //закрытие клавиатуры
    private fun closeKeyboard(view: View){
        val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        manager.hideSoftInputFromWindow(view.windowToken, 0)
    }
}