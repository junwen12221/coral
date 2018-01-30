package com.linkedin.coral.hive.hive2rel;

import com.google.common.base.Preconditions;
import com.linkedin.coral.hive.hive2rel.parsetree.ParseTreeBuilder;
import java.util.List;
import javax.annotation.Nonnull;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.rel.RelRoot;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.util.Util;
import org.apache.hadoop.hive.metastore.api.Table;


/**
 * Class that implements {@link org.apache.calcite.plan.RelOptTable.ViewExpander}
 * interface to support expansion of Hive Views to relational algebra.
 */
public class HiveViewExpander implements RelOptTable.ViewExpander {

  private final RelContextProvider relContextProvider;

  /**
   * Instantiates a new Hive view expander.
   *
   * @param relContextProvider Rel context provider instance
   */
  public HiveViewExpander(@Nonnull RelContextProvider relContextProvider) {
    Preconditions.checkNotNull(relContextProvider);
    this.relContextProvider = relContextProvider;
  }

  @Override
  public RelRoot expandView(RelDataType rowType, String queryString, List<String> schemaPath, List<String> viewPath) {
    Preconditions.checkNotNull(viewPath);
    Preconditions.checkState(!viewPath.isEmpty());

    HiveMetastoreClient msc = relContextProvider.getHiveSchema().getHiveMetastoreClient();
    String dbName = Util.last(schemaPath);
    String tableName = viewPath.get(0);
    Table table = msc.getTable(dbName, tableName);
    if (table == null) {
      throw new RuntimeException(String.format("Table %s.%s not found", dbName, tableName));
    }
    ParseTreeBuilder treeBuilder = new ParseTreeBuilder(msc,
        relContextProvider.getParseTreeBuilderConfig());
    SqlNode viewNode = treeBuilder.processView(table);
    return relContextProvider.getSqlToRelConverter().convertQuery(viewNode, true, true);
  }
}