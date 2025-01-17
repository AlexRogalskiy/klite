package klite.jdbc

import com.zaxxer.hikari.pool.HikariPool
import klite.Config
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import javax.sql.DataSource

abstract class DBTest {
  companion object {
    init {
      Config.useEnvFile()
      Config["ENV"] = "test"
    }

    val db: DataSource by lazy {
      try { DBModule().dataSource }
      catch (e: HikariPool.PoolInitializationException) {
        error("Test DB not running, please use `docker-compose up -d db`\n${e.message}")
      }
    }
  }

  @BeforeEach open fun startTx() {
    Transaction(db).attachToThread()
  }

  @AfterEach open fun rollbackTx() {
    Transaction.current()!!.close(commit = false)
  }
}
