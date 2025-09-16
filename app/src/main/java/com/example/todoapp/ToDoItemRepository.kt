package com.example.todoapp

import android.content.Context
import android.icu.text.Transliterator.Position
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Date
import java.util.LinkedHashSet

//перечисление приоритетов
enum class Priority(val value: Byte) {
    LOW(1), USUAL(2), EMERGENCY(3)
}

//Data-класс хранящий информацию об отдельном деле из списка
data class ToDoItem(
        val id: Int,
        val mainText: String,
        val itemCreateDate: Date,
        val priority: Priority = Priority.USUAL,
        val check: Boolean = false,
        val itemChangeDate: Date? = null){

    fun checkChange(check: Boolean): ToDoItem {
        return if (this.check == check)
            this
        else
            ToDoItem(id, mainText, itemCreateDate, priority, check, itemChangeDate)
    }

    fun change(itemChangeDate: Date): ToDoItem {
        return ToDoItem(id, mainText, itemCreateDate, priority, check, itemChangeDate)
    }
    fun change(itemChangeDate: Date, mainText: String): ToDoItem {
        return ToDoItem(id, mainText, itemCreateDate, priority, check, itemChangeDate)
    }
    fun change(itemChangeDate: Date, priority: Priority): ToDoItem {
        return ToDoItem(id, mainText, itemCreateDate, priority, check, itemChangeDate)
    }
}


class ToDoItemRepository(private val dataStore: DataStore<Preferences>, val lifecycleScope: LifecycleCoroutineScope, val context: Context){

    private val TAG: String ="ToDoItemRep"

    val zeroItem = ToDoItem(0, "damage data", Date(0L), Priority.USUAL, false, Date(0L))

    lateinit var sortOrder: Order

    private val toDoItemMap: MutableMap<Int, ToDoItem> = mutableMapOf(zeroItem.id to zeroItem)
    fun getToDoItemMap(): Map<Int, ToDoItem> = toDoItemMap.toMap()
    fun getToDoItemPerId(id: Int): ToDoItem? = toDoItemMap[id]
    fun getToDoItemPerPosition(order: Order, position: Int): ToDoItem{
        val ret: ToDoItem?
        when(order){
            Order.CreateDate->
                ret = toDoItemMap[sortCreateDate[position]]
            Order.CreateDateCheck->
                ret = toDoItemMap[sortCreateDateCheck[position]]
            Order.CreateDatePriority->
                ret = toDoItemMap[sortCreateDatePriority[position]]
            Order.CreateDateCheckPriority->
                ret = toDoItemMap[sortCreateDateCheckPriority[position]]
            Order.ChangeDate->
                ret = toDoItemMap[sortChangeDate[position]]
            Order.ChangeDateCheck->
                ret = toDoItemMap[sortChangeDateCheck[position]]
            Order.ChangeDatePriority->
                ret = toDoItemMap[sortChangeDatePriority[position]]
            Order.ChangeDateCheckPriority->
                ret = toDoItemMap[sortChangeDateCheckPriority[position]]
            Order.RevCreateDate->
                ret = toDoItemMap[sortCreateDate[sortCreateDate.size - position - 1]]
            Order.RevCreateDateCheck-> {
                ret = if(position < positionFirstCheck)
                    toDoItemMap[sortCreateDateCheck[positionFirstCheck - position - 1]]
                else
                    toDoItemMap[sortCreateDateCheck[sortCreateDateCheck.size + positionFirstCheck - position - 1]]
            }
            Order.RevCreateDatePriority->
                ret = if(position < positionFirstUsual)
                    toDoItemMap[sortCreateDatePriority[positionFirstUsual - position - 1]]
                else if(position < positionFirstLow)
                    toDoItemMap[sortCreateDatePriority[positionFirstLow + positionFirstUsual - position - 1]]
                else
                    toDoItemMap[sortCreateDatePriority[sortCreateDatePriority.size + positionFirstLow - position - 1]]
            Order.RevCreateDateCheckPriority->
                ret = if(position < positionFirstCheck){
                    if(position < positionFirstUsualUnCheck)
                        toDoItemMap[sortCreateDateCheckPriority[positionFirstUsualUnCheck - position - 1]]
                    else if(position < positionFirstLowUnCheck)
                        toDoItemMap[sortCreateDateCheckPriority[positionFirstLowUnCheck + positionFirstUsualUnCheck - position - 1]]
                    else
                        toDoItemMap[sortCreateDateCheckPriority[positionFirstCheck + positionFirstLowUnCheck - position - 1]]
                }
                else{
                    if(position < positionFirstUsualCheck)
                        toDoItemMap[sortCreateDateCheckPriority[positionFirstUsualCheck + positionFirstCheck - position - 1]]
                    else if(position < positionFirstLowCheck)
                        toDoItemMap[sortCreateDateCheckPriority[positionFirstLowCheck + positionFirstUsualCheck - position - 1]]
                    else
                        toDoItemMap[sortCreateDateCheckPriority[sortCreateDateCheckPriority.size + positionFirstLowCheck - position - 1]]
                }
            Order.RevChangeDate->
                ret = toDoItemMap[sortChangeDate[sortChangeDate.size - position - 1]]
            Order.RevChangeDateCheck-> {
                ret = if(position < positionFirstCheck)
                    toDoItemMap[sortChangeDateCheck[positionFirstCheck - position - 1]]
                else
                    toDoItemMap[sortChangeDateCheck[sortChangeDateCheck.size + positionFirstCheck - position - 1]]
            }
            Order.RevChangeDatePriority->
                ret = if(position < positionFirstUsual)
                    toDoItemMap[sortChangeDatePriority[positionFirstUsual - position - 1]]
                else if(position < positionFirstLow)
                    toDoItemMap[sortChangeDatePriority[positionFirstLow + positionFirstUsual - position - 1]]
                else
                    toDoItemMap[sortChangeDatePriority[sortChangeDatePriority.size + positionFirstLow - position - 1]]
            Order.RevChangeDateCheckPriority->
                ret = if(position < positionFirstCheck){
                    if(position < positionFirstUsualUnCheck)
                        toDoItemMap[sortChangeDateCheckPriority[positionFirstUsualUnCheck - position - 1]]
                    else if(position < positionFirstLowUnCheck)
                        toDoItemMap[sortChangeDateCheckPriority[positionFirstLowUnCheck + positionFirstUsualUnCheck - position - 1]]
                    else
                        toDoItemMap[sortChangeDateCheckPriority[positionFirstCheck + positionFirstLowUnCheck - position - 1]]
                }
                else{
                    if(position < positionFirstUsualCheck)
                        toDoItemMap[sortChangeDateCheckPriority[positionFirstUsualCheck + positionFirstCheck - position - 1]]
                    else if(position < positionFirstLowCheck)
                        toDoItemMap[sortChangeDateCheckPriority[positionFirstLowCheck + positionFirstUsualCheck - position - 1]]
                    else
                        toDoItemMap[sortChangeDateCheckPriority[sortChangeDateCheckPriority.size + positionFirstLowCheck - position - 1]]
                }

        }
        return ret ?: zeroItem
    }
    fun getPositionPerToDoItem(toDoItemId: Int): Int?{
        return getPositionPerToDoItem(sortOrder, toDoItemId)
    }
    fun getPositionPerToDoItem(order: Order, toDoItemId: Int): Int?{
        var ret: Int? = null
        when(order){
            Order.CreateDate->
                ret = sortCreateDateMap[toDoItemId]
            Order.CreateDateCheck->
                ret = sortCreateDateCheckMap[toDoItemId]
            Order.CreateDatePriority->
                ret = sortCreateDatePriorityMap[toDoItemId]
            Order.CreateDateCheckPriority->
                ret = sortCreateDateCheckPriorityMap[toDoItemId]
            Order.ChangeDate->
                ret = sortChangeDateMap[toDoItemId]
            Order.ChangeDateCheck->
                ret = sortChangeDateCheckMap[toDoItemId]
            Order.ChangeDatePriority->
                ret = sortChangeDatePriorityMap[toDoItemId]
            Order.ChangeDateCheckPriority->
                ret = sortChangeDateCheckPriorityMap[toDoItemId]
            Order.RevCreateDate->
                if(sortCreateDateMap.containsKey(toDoItemId))
                    ret = sortCreateDate.size - sortCreateDateMap[toDoItemId]!! - 1
            Order.RevCreateDateCheck->
                ret = if(sortCreateDateCheckMap.containsKey(toDoItemId))
                    if(sortCreateDateCheckMap[toDoItemId]!! < positionFirstCheck)
                        positionFirstCheck - sortCreateDateCheckMap[toDoItemId]!! - 1
                    else
                        sortCreateDateCheck.size + positionFirstCheck - sortCreateDateCheckMap[toDoItemId]!! - 1
                else
                    null
            Order.RevCreateDatePriority->
                ret = if(sortCreateDatePriorityMap.containsKey(toDoItemId))
                    if(sortCreateDatePriorityMap[toDoItemId]!! < positionFirstUsual)
                        positionFirstUsual - sortCreateDatePriorityMap[toDoItemId]!! - 1
                    else if(sortCreateDatePriorityMap[toDoItemId]!! < positionFirstLow)
                        positionFirstLow + positionFirstUsual - sortCreateDatePriorityMap[toDoItemId]!! - 1
                    else
                        sortCreateDatePriority.size + positionFirstLow - sortCreateDatePriorityMap[toDoItemId]!! - 1
                else
                    null
            Order.RevCreateDateCheckPriority->
                ret = if(sortCreateDateCheckPriorityMap.containsKey(toDoItemId))
                    if(sortCreateDateCheckPriorityMap[toDoItemId]!! < positionFirstCheck)
                        if(sortCreateDateCheckPriorityMap[toDoItemId]!! < positionFirstUsualUnCheck)
                            positionFirstUsualUnCheck - sortCreateDateCheckPriorityMap[toDoItemId]!! - 1
                        else if(sortCreateDateCheckPriorityMap[toDoItemId]!! < positionFirstLowUnCheck)
                            positionFirstLowUnCheck + positionFirstUsualUnCheck - sortCreateDateCheckPriorityMap[toDoItemId]!! - 1
                        else
                            positionFirstCheck + positionFirstLowUnCheck - sortCreateDateCheckPriorityMap[toDoItemId]!! - 1
                    else
                        if(sortCreateDateCheckPriorityMap[toDoItemId]!! < positionFirstUsualCheck)
                            positionFirstUsualCheck + positionFirstCheck - sortCreateDateCheckPriorityMap[toDoItemId]!! - 1
                        else if(sortCreateDateCheckPriorityMap[toDoItemId]!! < positionFirstLowCheck)
                            positionFirstLowCheck + positionFirstUsualCheck - sortCreateDateCheckPriorityMap[toDoItemId]!! - 1
                        else
                            sortCreateDateCheckPriority.size + positionFirstLowCheck - sortCreateDateCheckPriorityMap[toDoItemId]!! - 1
                else
                    null
            Order.RevChangeDate->
                if(sortChangeDateMap.containsKey(toDoItemId))
                    ret = sortChangeDate.size - sortChangeDateMap[toDoItemId]!! - 1
            Order.RevChangeDateCheck->
                ret = if (sortChangeDateCheckMap.containsKey(toDoItemId))
                    if(sortChangeDateCheckMap[toDoItemId]!! < positionFirstCheck)
                        positionFirstCheck - sortChangeDateCheckMap[toDoItemId]!! - 1
                    else
                        sortChangeDateCheck.size + positionFirstCheck - sortChangeDateCheckMap[toDoItemId]!! - 1
                else
                    null
            Order.RevChangeDatePriority->
                ret = if(sortChangeDatePriorityMap.containsKey(toDoItemId))
                    if(sortChangeDatePriorityMap[toDoItemId]!! < positionFirstUsual)
                        positionFirstUsual - sortChangeDatePriorityMap[toDoItemId]!! - 1
                    else if(sortChangeDatePriorityMap[toDoItemId]!! < positionFirstLow)
                        positionFirstLow + positionFirstUsual - sortChangeDatePriorityMap[toDoItemId]!! - 1
                    else
                        sortChangeDatePriority.size + positionFirstLow - sortChangeDatePriorityMap[toDoItemId]!! - 1
                else
                    null
            Order.RevChangeDateCheckPriority->
                ret = if(sortChangeDateCheckPriorityMap.containsKey(toDoItemId))
                        if(sortChangeDateCheckPriorityMap[toDoItemId]!! < positionFirstCheck)
                            if(sortChangeDateCheckPriorityMap[toDoItemId]!! < positionFirstUsualUnCheck)
                                positionFirstUsualUnCheck - sortChangeDateCheckPriorityMap[toDoItemId]!! - 1
                            else if(sortChangeDateCheckPriorityMap[toDoItemId]!! < positionFirstLowUnCheck)
                                positionFirstLowUnCheck + positionFirstUsualUnCheck - sortChangeDateCheckPriorityMap[toDoItemId]!! - 1
                            else
                                positionFirstCheck + positionFirstLowUnCheck - sortChangeDateCheckPriorityMap[toDoItemId]!! - 1
                        else
                            if(sortChangeDateCheckPriorityMap[toDoItemId]!! < positionFirstUsualCheck)
                                positionFirstUsualCheck + positionFirstCheck - sortChangeDateCheckPriorityMap[toDoItemId]!! - 1
                            else if(sortChangeDateCheckPriorityMap[toDoItemId]!! < positionFirstLowCheck)
                                positionFirstLowCheck + positionFirstUsualCheck - sortChangeDateCheckPriorityMap[toDoItemId]!! - 1
                            else
                                sortChangeDateCheckPriority.size + positionFirstLowCheck - sortChangeDateCheckPriorityMap[toDoItemId]!! - 1
                else
                    null
        }
            return ret
    }


    private val sortCreateDate = ArrayList<Int>()
    private val sortCreateDateMap = HashMap<Int, Int>()
    private val sortCreateDateCheck = ArrayList<Int>()
    private val sortCreateDateCheckMap = HashMap<Int, Int>()
    private val sortCreateDatePriority = ArrayList<Int>()
    private val sortCreateDatePriorityMap = HashMap<Int, Int>()
    private val sortCreateDateCheckPriority = ArrayList<Int>()
    private val sortCreateDateCheckPriorityMap = HashMap<Int, Int>()
    private val sortChangeDate = ArrayList<Int>()
    private val sortChangeDateMap = HashMap<Int, Int>()
    private val sortChangeDateCheck = ArrayList<Int>()
    private val sortChangeDateCheckMap = HashMap<Int, Int>()
    private val sortChangeDatePriority = ArrayList<Int>()
    private val sortChangeDatePriorityMap = HashMap<Int, Int>()
    private val sortChangeDateCheckPriority = ArrayList<Int>()
    private val sortChangeDateCheckPriorityMap = HashMap<Int, Int>()
    private var positionFirstCheck: Int = 0
        get() = field
    private var positionFirstUsual: Int = 0
        get() = field
    private var positionFirstLow: Int = 0
        get() = field
    private var positionFirstUsualUnCheck: Int = 0
        get() = field
    private var positionFirstLowUnCheck: Int = 0
        get() = field
    private var positionFirstUsualCheck: Int = 0
        get() = field
    private var positionFirstLowCheck: Int = 0
        get() = field



    private object KeysNames { //ключи для сохранения дел
        //const val ID = "id"
        const val MAIN_TEXT = "main_text"
        const val ITEM_CREATE_DATE = "item_create_date"
        const val PRIORITY = "priority"
        const val CHECK = "check"
        const val ITEM_CHANGE_DATE = "item_change_date"
    }

    private var maxId = -1
    private var minId = 0
    private val nextId: LinkedHashSet<Int> = linkedSetOf()
    private val nextIdMaxSize = 3
    private val usedId: MutableSet<Int> = mutableSetOf() //Id выданные nextId(), но ещё не добавленные в toDoItemMap
    private var goodJob: Job? = null

    //функция быстро выдающая следующий ID
    fun nextId(): Int{
        val ret: Int
        if(nextId.isNotEmpty()){
            ret = nextId.first()
            nextId.remove(ret)
        }
        else
            ret = ++maxId
        usedId.add(ret)
        if(goodJob == null && nextId.size<nextIdMaxSize) //запуск updateNextId(), если не запущена
            goodJob = lifecycleScope.launch {
                updateNextId()
            }
        return ret
    }

    //функция пополнения ID для быстрой выдачи
    private fun updateNextId(){
        var i = 0
        while(nextId.size<nextIdMaxSize)
            if(!toDoItemMap.contains(++i) && !usedId.contains(i))
                nextId.add(i)
        goodJob = null
    }


    /*constructor(dataStore: DataStore<Preferences>, lifecycleScope: LifecycleCoroutineScope): this(dataStore){
        lifecycleScope.launch {
            var id: Int
            var j: Int
            val preferences = dataStore.data.first().toPreferences()
            for(key in preferences.asMap().keys) {
                id = 0
                j = 0
                do {
                    id = 10 * id + key.name[j++].code - 48
                } while (key.name[j] in '0'..'9')
                if(!toDoItemMap.contains(id))
                    toDoItemMap[id] = mapToDoItemPerId(id, preferences)
            }
        }
    }*/

    private fun mapChangeToDoItems(preferences: Preferences): Array<ArrayList<Int>>{
        val changeToDoItemSet: MutableSet<Int> = mutableSetOf() //дела в которых есть изменения
        val newToDoItemSet: MutableSet<Int> = mutableSetOf() //новые дела
        val deletedToDoItemSet: MutableSet<Int> = mutableSetOf() //удалённые дела
        for(n in toDoItemMap.keys)
            deletedToDoItemSet.add(n)
        deletedToDoItemSet.remove(0)
        var changePriority: Boolean = false
        var changeCheck: Boolean = false
        var changeChangeDate: Boolean = false
        var changeCreateDate: Boolean = false

        var id: Int
        var j: Int
        maxId = 0
        minId = Int.MAX_VALUE

        for(key in preferences.asMap().keys) {
            id = 0
            j = 0
            do {
                id = 10 * id + key.name[j++].code - 48
            } while (key.name[j] in '0'..'9')

            if(id>maxId)
                maxId = id
            if(id<minId)
                minId = id

            if(!toDoItemMap.contains(id)){
                val toDoItem = mapToDoItemPerId(id, preferences)
                toDoItemMap[id] = toDoItem
                usedId.remove(id)
                nextId.remove(id)
                newToDoItemSet.add(id)

                changeCreateDate = true
                sortCreateDate.add(id)
                sortCreateDateCheck.add(id)
                sortCreateDatePriority.add(id)
                sortCreateDateCheckPriority.add(id)
                sortChangeDate.add(id)
                sortChangeDateCheck.add(id)
                sortChangeDatePriority.add(id)
                sortChangeDateCheckPriority.add(id)
            }
            else{
                deletedToDoItemSet.remove(id)
                when(key.name.substring(j)){
                    KeysNames.MAIN_TEXT -> if(preferences[key] != toDoItemMap[id]!!.mainText){
                        val toDoItem = toDoItemMap[id]!!.change(Date(preferences[longPreferencesKey(id.toString()+KeysNames.ITEM_CHANGE_DATE)] ?: zeroItem.itemChangeDate!!.time), preferences[key] as String)
                        toDoItemMap[id] = toDoItem
                        changeToDoItemSet.add(id)
                    }
                    KeysNames.PRIORITY ->if(preferences[key] != toDoItemMap[id]!!.priority.toString()){
                        val toDoItem = toDoItemMap[id]!!.change(Date(preferences[longPreferencesKey(id.toString()+KeysNames.ITEM_CHANGE_DATE)] ?: zeroItem.itemChangeDate!!.time), Priority.valueOf(preferences[key] as String))
                        toDoItemMap[id] = toDoItem
                        changeToDoItemSet.add(id)
                        changePriority = true
                    }
                    KeysNames.CHECK ->if(preferences[key] != toDoItemMap[id]!!.check){
                        val toDoItem = toDoItemMap[id]!!.checkChange(preferences[key] as Boolean)
                        toDoItemMap[id] = toDoItem
                        changeToDoItemSet.add(id)
                        changeCheck = true
                    }
                    KeysNames.ITEM_CHANGE_DATE ->if(preferences[key] != 0L && preferences[key] != toDoItemMap[id]!!.itemChangeDate){
                        val toDoItem = toDoItemMap[id]!!.change(Date(preferences[key] as Long))
                        toDoItemMap[id] = toDoItem
                        changeToDoItemSet.add(id)
                        changeChangeDate = true
                    }
                }
            }
        }

        val deletedPositions = arrayListOf<Int>()
        for(n in deletedToDoItemSet) {
            toDoItemMap.remove(n)
            deletedPositions.add(getPositionPerToDoItem(n) ?: -1)
        }
        deletedPositions.remove(-1)

        val oldChangePositions = arrayListOf<Int>()
        for(n in changeToDoItemSet){
            oldChangePositions.add(getPositionPerToDoItem(n) ?: -1)
        }
        oldChangePositions.remove(-1)


        //запускаем сортировки изменённыйх массивов
        if(!changeCreateDate){
            if(changeCheck)
                sortCreateDateCheck()
            if(changePriority)
                sortCreateDatePriority()
            if(changeCheck || changePriority)
                sortCreateDateCheckPriority()
            if(changeChangeDate)
                sortChangeDate()
            if(changeChangeDate || changeCheck)
                sortChangeDateCheck()
            if(changeChangeDate || changePriority)
                sortChangeDatePriority()
            if(changeChangeDate || changeCheck || changePriority)
                sortChangeDateCheckPriority()
        }
        else{
            sortCreateDate()
            sortCreateDateCheck()
            sortCreateDatePriority()
            sortCreateDateCheckPriority()
            sortChangeDate()
            sortChangeDateCheck()
            sortChangeDatePriority()
            sortChangeDateCheckPriority()
        }

        val newPositions = arrayListOf<Int>()
        for(n in newToDoItemSet){
            newPositions.add(getPositionPerToDoItem(n) ?: -1)
        }
        newPositions.remove(-1)

        val newChangePositions = arrayListOf<Int>()
        for(n in changeToDoItemSet){
            newChangePositions.add(getPositionPerToDoItem(n) ?: -1)
        }
        newChangePositions.remove(-1)

        return arrayOf(deletedPositions, newPositions, oldChangePositions, newChangePositions)
    }



    // распаковка дела из памяти
    private fun mapToDoItemPerId(_id: Int, preferences: Preferences): ToDoItem{
        val id = _id.toString()
        val mainText = preferences[stringPreferencesKey(id+KeysNames.MAIN_TEXT)] ?: zeroItem.mainText
        val itemCreateDate = Date(preferences[longPreferencesKey(id+KeysNames.ITEM_CREATE_DATE)] ?: zeroItem.itemCreateDate.time)
        val priority = Priority.valueOf(preferences[stringPreferencesKey(id+KeysNames.PRIORITY)] ?: zeroItem.priority.toString())
        val check = preferences[booleanPreferencesKey(id+KeysNames.CHECK)] ?: zeroItem.check
        val itemChangeInt = preferences[longPreferencesKey(id+KeysNames.ITEM_CHANGE_DATE)] ?: zeroItem.itemChangeDate!!.time
        var itemChangeDate: Date? = null
        if(itemChangeInt != 0L)
            itemChangeDate = Date(itemChangeInt)
        return ToDoItem(_id, mainText, itemCreateDate, priority, check, itemChangeDate)
    }

    //поток дел
    val toDoItemRepositoryFlow: Flow<Array<ArrayList<Int>>> = dataStore.data
        .catch {exception ->
            if(exception is IOException){
                Log.e(TAG, "Error reading preferences.", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.transform { preferences ->
            emit(mapChangeToDoItems(preferences))
        }

    // добавление дела
    fun addToDoItem(toDoItem: ToDoItem){
        val id = toDoItem.id.toString()
        if(toDoItemMap.contains(toDoItem.id))
            throw Exception("attempt use addToDoItem() to edit toDoItem")
        else{
            nextId.remove(toDoItem.id)
            usedId.add(toDoItem.id)
            lifecycleScope.launch {
                dataStore.edit { preferences ->
                    preferences[stringPreferencesKey(id+KeysNames.MAIN_TEXT)] = toDoItem.mainText
                    preferences[longPreferencesKey(id+KeysNames.ITEM_CREATE_DATE)] = toDoItem.itemCreateDate.time
                    preferences[stringPreferencesKey(id+KeysNames.PRIORITY)] = toDoItem.priority.toString()
                    preferences[booleanPreferencesKey(id+KeysNames.CHECK)] = toDoItem.check
                    preferences[longPreferencesKey(id+KeysNames.ITEM_CHANGE_DATE)] = toDoItem.itemChangeDate?.time ?: 0L
                }
            }
        }
    }

    //изменение галочки
    fun editToDoItem(id: Int, check: Boolean){
        if(toDoItemMap.contains(id))
            lifecycleScope.launch {
                dataStore.edit { preferences ->
                    preferences[booleanPreferencesKey(id.toString()+KeysNames.CHECK)] = check
                }
            }
    }

    //изменение текста
    fun editToDoItem(id: Int, mainText: String){
        val _id = id.toString()
        if(toDoItemMap.contains(id))
            lifecycleScope.launch {
                dataStore.edit { preferences ->
                    preferences[stringPreferencesKey(_id+KeysNames.MAIN_TEXT)] = mainText
                    preferences[longPreferencesKey(_id+KeysNames.ITEM_CHANGE_DATE)] = Date().time
                }
            }
    }

    //изменение текста и приоретета
    fun editToDoItem(id: Int, mainText: String, priority: Priority){
        val _id = id.toString()
        if(toDoItemMap.contains(id))
            lifecycleScope.launch {
                dataStore.edit { preferences ->
                    preferences[stringPreferencesKey(_id+KeysNames.MAIN_TEXT)] = mainText
                    preferences[stringPreferencesKey(_id+KeysNames.PRIORITY)] = priority.toString()
                    preferences[longPreferencesKey(_id+KeysNames.ITEM_CHANGE_DATE)] = Date().time
                }
            }
    }

    //изменение приоретета
    fun editToDoItem(id: Int, priority: Priority){
        val _id = id.toString()
        if(toDoItemMap.contains(id))
            lifecycleScope.launch {
                dataStore.edit { preferences ->
                    preferences[stringPreferencesKey(_id+KeysNames.PRIORITY)] = priority.toString()
                    preferences[longPreferencesKey(_id+KeysNames.ITEM_CHANGE_DATE)] = Date().time
                }
            }
    }

    fun removeToDoItem(id: Int){
        val id = id.toString()
        lifecycleScope.launch{
            dataStore.edit {
                it.remove(stringPreferencesKey(id+KeysNames.MAIN_TEXT))
                it.remove(longPreferencesKey(id+KeysNames.ITEM_CREATE_DATE))
                it.remove(stringPreferencesKey(id+KeysNames.PRIORITY))
                it.remove(booleanPreferencesKey(id+KeysNames.CHECK))
                it.remove(longPreferencesKey(id+KeysNames.ITEM_CHANGE_DATE))
            }
        }
    }

    //сортировки вставками
    private fun sortCreateDate(){
        var save: Int
        var j: Int
        for(i: Int in 1..<sortCreateDate.size){
            j=i
            while(j>0 && toDoItemMap[sortCreateDate[j]]!!.itemCreateDate < toDoItemMap[sortCreateDate[j-1]]!!.itemCreateDate){
                save = sortCreateDate[j]
                sortCreateDate[j] = sortCreateDate[j-1]
                sortCreateDate[j---1] = save
            }
        }
        for(n: Int in 0..<sortCreateDate.size)
            sortCreateDateMap[sortCreateDate[n]] = n
    }
    private fun sortCreateDateCheck(){
        var save: Int
        var j: Int
        positionFirstCheck = 0
        if (!toDoItemMap[sortCreateDateCheck[0]]!!.check)
            positionFirstCheck++
        for(i: Int in 1..<sortCreateDateCheck.size){
            if (!toDoItemMap[sortCreateDateCheck[i]]!!.check)
                positionFirstCheck++
            j=i
            // меняем если неотсортированный элемент unCheck
            while(j>0 && (!toDoItemMap[sortCreateDateCheck[j]]!!.check && toDoItemMap[sortCreateDateCheck[j-1]]!!.check
                        // или меняем если неосортированный элемент старше
                        || toDoItemMap[sortCreateDateCheck[j]]!!.itemCreateDate < toDoItemMap[sortCreateDateCheck[j-1]]!!.itemCreateDate
                        // но только если Check\unCheck совпадают
                        && toDoItemMap[sortCreateDateCheck[j]]!!.check == toDoItemMap[sortCreateDateCheck[j-1]]!!.check)){
                save = sortCreateDateCheck[j]
                sortCreateDateCheck[j] = sortCreateDateCheck[j-1]
                sortCreateDateCheck[j---1] = save
            }
        }
        for(n in 0..<sortCreateDateCheck.size)
            sortCreateDateCheckMap[sortCreateDateCheck[n]] = n
    }
    private fun sortCreateDatePriority(){
        var save: Int
        var j: Int
        positionFirstUsual = 0
        positionFirstLow = 0
        if (toDoItemMap[sortCreateDatePriority[0]]!!.priority==Priority.EMERGENCY){
            positionFirstUsual++
            positionFirstLow++
        }
        else if (toDoItemMap[sortCreateDatePriority[0]]!!.priority==Priority.USUAL)
            positionFirstLow++
        for(i: Int in 1..<sortCreateDatePriority.size){
            if (toDoItemMap[sortCreateDatePriority[i]]!!.priority==Priority.EMERGENCY){
                positionFirstUsual++
                positionFirstLow++
            }
            else if (toDoItemMap[sortCreateDatePriority[i]]!!.priority==Priority.USUAL)
                positionFirstLow++
            j=i
            // меняем если у неотсортированного элемента выше приоритет
            while(j>0 && (toDoItemMap[sortCreateDatePriority[j]]!!.priority > toDoItemMap[sortCreateDatePriority[j-1]]!!.priority
                        // или меняем если неосортированный элемент старше
                        || toDoItemMap[sortCreateDatePriority[j]]!!.itemCreateDate < toDoItemMap[sortCreateDatePriority[j-1]]!!.itemCreateDate
                        // но только если приоритет одинаковый
                        && (toDoItemMap[sortCreateDatePriority[j]]!!.priority == toDoItemMap[sortCreateDatePriority[j-1]]!!.priority))){
                save = sortCreateDatePriority[j]
                sortCreateDatePriority[j] = sortCreateDatePriority[j-1]
                sortCreateDatePriority[j---1] = save
            }
        }
        for(n in 0..<sortCreateDatePriority.size)
            sortCreateDatePriorityMap[sortCreateDatePriority[n]] = n

    }
    private fun sortCreateDateCheckPriority(){
        var save: Int
        var j: Int
        positionFirstUsualUnCheck = 0
        positionFirstLowUnCheck = 0
        positionFirstUsualCheck = 0
        positionFirstLowCheck = 0
        if(!toDoItemMap[sortCreateDateCheckPriority[0]]!!.check){
            positionFirstUsualCheck++
            positionFirstLowCheck++
            if(toDoItemMap[sortCreateDateCheckPriority[0]]!!.priority == Priority.EMERGENCY){
                positionFirstUsualUnCheck++
                positionFirstLowUnCheck++
            }
            else if(toDoItemMap[sortCreateDateCheckPriority[0]]!!.priority == Priority.USUAL)
                positionFirstLowUnCheck++
        }
        else {
            if(toDoItemMap[sortCreateDateCheckPriority[0]]!!.priority == Priority.EMERGENCY){
                positionFirstUsualCheck++
                positionFirstLowCheck++
            }
            else if(toDoItemMap[sortCreateDateCheckPriority[0]]!!.priority == Priority.USUAL)
                positionFirstLowCheck++
        }

        for(i: Int in 1..<sortCreateDateCheckPriority.size){
            if(!toDoItemMap[sortCreateDateCheckPriority[i]]!!.check){
                positionFirstUsualCheck++
                positionFirstLowCheck++
                if(toDoItemMap[sortCreateDateCheckPriority[i]]!!.priority == Priority.EMERGENCY){
                    positionFirstUsualUnCheck++
                    positionFirstLowUnCheck++
                }
                else if(toDoItemMap[sortCreateDateCheckPriority[i]]!!.priority == Priority.USUAL)
                    positionFirstLowUnCheck++
            }
            else {
                if(toDoItemMap[sortCreateDateCheckPriority[i]]!!.priority == Priority.EMERGENCY){
                    positionFirstUsualCheck++
                    positionFirstLowCheck++
                }
                else if(toDoItemMap[sortCreateDateCheckPriority[i]]!!.priority == Priority.USUAL)
                    positionFirstLowCheck++
            }
            j=i
            // меняем если неотсортированный элемент unCheck
            while(j>0 && (!toDoItemMap[sortCreateDateCheckPriority[j]]!!.check && toDoItemMap[sortCreateDateCheckPriority[j-1]]!!.check
                        // или если у неотсортированного больше приоритет
                        || toDoItemMap[sortCreateDateCheckPriority[j]]!!.priority > toDoItemMap[sortCreateDateCheckPriority[j-1]]!!.priority
                        // но только если Check\unCheck совпадают
                        && toDoItemMap[sortCreateDateCheckPriority[j]]!!.check == toDoItemMap[sortCreateDateCheckPriority[j-1]]!!.check
                        // или меняем если неосортированный элемент старше
                        || toDoItemMap[sortCreateDateCheckPriority[j]]!!.itemCreateDate < toDoItemMap[sortCreateDateCheckPriority[j-1]]!!.itemCreateDate
                        // но только если Check\unCheck совпадают
                        && (toDoItemMap[sortCreateDateCheckPriority[j]]!!.check == toDoItemMap[sortCreateDateCheckPriority[j-1]]!!.check)
                        // и приоритет одинаковый
                        && toDoItemMap[sortCreateDateCheckPriority[j]]!!.priority == toDoItemMap[sortCreateDateCheckPriority[j-1]]!!.priority)){
                save = sortCreateDateCheckPriority[j]
                sortCreateDateCheckPriority[j] = sortCreateDateCheckPriority[j-1]
                sortCreateDateCheckPriority[j---1] = save
            }
        }
        for(n in 0..<sortCreateDateCheckPriority.size)
            sortCreateDateCheckPriorityMap[sortCreateDateCheckPriority[n]] = n
    }
    //тоже что и sortCreateDate, но с ChangeDate, если имеется
    private fun sortChangeDate(){
        var save: Int
        var j: Int
        for(i: Int in 1..<sortChangeDate.size){
            j=i
            while(j>0 && (toDoItemMap[sortChangeDate[j]]!!.itemChangeDate ?: toDoItemMap[sortChangeDate[j]]!!.itemCreateDate) < (toDoItemMap[sortChangeDate[j-1]]!!.itemChangeDate ?: toDoItemMap[sortChangeDate[j-1]]!!.itemCreateDate)){
                save = sortChangeDate[j]
                sortChangeDate[j] = sortChangeDate[j-1]
                sortChangeDate[j---1] = save
            }
        }
        for(n in 0..<sortChangeDate.size)
            sortChangeDateMap[sortChangeDate[n]] = n
    }
    private fun sortChangeDateCheck(){
        var save: Int
        var j: Int
        for(i: Int in 1..<sortChangeDateCheck.size){
            j=i
            // меняем если неотсортированный элемент unCheck
            while(j>0 && (!toDoItemMap[sortChangeDateCheck[j]]!!.check && toDoItemMap[sortChangeDateCheck[j-1]]!!.check
                        // или меняем если неосортированный элемент старше
                        || (toDoItemMap[sortChangeDateCheck[j]]!!.itemChangeDate ?: toDoItemMap[sortChangeDateCheck[j]]!!.itemCreateDate) < (toDoItemMap[sortChangeDateCheck[j-1]]!!.itemChangeDate ?: toDoItemMap[sortChangeDateCheck[j-1]]!!.itemCreateDate)
                        // но только если Check\unCheck совпадают
                        && toDoItemMap[sortChangeDateCheck[j]]!!.check == toDoItemMap[sortChangeDateCheck[j-1]]!!.check)){
                save = sortChangeDateCheck[j]
                sortChangeDateCheck[j] = sortChangeDateCheck[j-1]
                sortChangeDateCheck[j---1] = save
            }
        }
        for(n in 0..<sortChangeDateCheck.size)
            sortChangeDateCheckMap[sortChangeDateCheck[n]] = n
    }
    private fun sortChangeDatePriority(){
        var save: Int
        var j: Int
        for(i: Int in 1..<sortChangeDatePriority.size){
            j=i
            // меняем если у неотсортированного элемента выше приоритет
            while(j>0 && (toDoItemMap[sortChangeDatePriority[j]]!!.priority > toDoItemMap[sortChangeDatePriority[j-1]]!!.priority
                        // или меняем если неосортированный элемент старше
                        || (toDoItemMap[sortChangeDatePriority[j]]!!.itemChangeDate ?: toDoItemMap[sortChangeDatePriority[j]]!!.itemCreateDate) < (toDoItemMap[sortChangeDatePriority[j-1]]!!.itemChangeDate ?: toDoItemMap[sortChangeDatePriority[j-1]]!!.itemCreateDate)
                        // но только если приоритет одинаковый
                        && (toDoItemMap[sortChangeDatePriority[j]]!!.priority == toDoItemMap[sortChangeDatePriority[j-1]]!!.priority))){
                save = sortChangeDatePriority[j]
                sortChangeDatePriority[j] = sortChangeDatePriority[j-1]
                sortChangeDatePriority[j---1] = save
            }
        }
        for(n in 0..<sortChangeDatePriority.size)
            sortChangeDatePriorityMap[sortChangeDatePriority[n]] = n
    }
    private fun sortChangeDateCheckPriority(){
        var save: Int
        var j: Int
        for(i: Int in 1..<sortChangeDateCheckPriority.size){
            j=i
            // меняем если неотсортированный элемент unCheck
            while(j>0 && (!toDoItemMap[sortChangeDateCheckPriority[j]]!!.check && toDoItemMap[sortChangeDateCheckPriority[j-1]]!!.check
                        // или если у неотсортированного больше приоритет
                        || toDoItemMap[sortChangeDateCheckPriority[j]]!!.priority > toDoItemMap[sortChangeDateCheckPriority[j-1]]!!.priority
                        // но только если Check\unCheck совпадают
                        && toDoItemMap[sortChangeDateCheckPriority[j]]!!.check == toDoItemMap[sortChangeDateCheckPriority[j-1]]!!.check
                        // или меняем если неосортированный элемент старше
                        || (toDoItemMap[sortChangeDateCheckPriority[j]]!!.itemChangeDate ?: toDoItemMap[sortChangeDateCheckPriority[j]]!!.itemCreateDate) < (toDoItemMap[sortChangeDateCheckPriority[j-1]]!!.itemChangeDate ?: toDoItemMap[sortChangeDateCheckPriority[j-1]]!!.itemCreateDate)
                        // но только если Check\unCheck совпадают
                        && (toDoItemMap[sortChangeDateCheckPriority[j]]!!.check == toDoItemMap[sortChangeDateCheckPriority[j-1]]!!.check)
                        // и приоритет одинаковый
                        && toDoItemMap[sortChangeDateCheckPriority[j]]!!.priority == toDoItemMap[sortChangeDateCheckPriority[j-1]]!!.priority)){
                save = sortChangeDateCheckPriority[j]
                sortChangeDateCheckPriority[j] = sortChangeDateCheckPriority[j-1]
                sortChangeDateCheckPriority[j---1] = save
            }
        }
        for(n in 0..<sortChangeDateCheckPriority.size)
            sortChangeDateCheckPriorityMap[sortChangeDateCheckPriority[n]] = n
    }

}

