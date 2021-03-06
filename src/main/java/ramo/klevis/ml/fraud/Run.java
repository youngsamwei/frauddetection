package ramo.klevis.ml.fraud;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import ramo.klevis.ml.fraud.algorithm.AlgorithmConfiguration;
import ramo.klevis.ml.fraud.algorithm.FraudDetectionAlgorithmJavaStream;
import ramo.klevis.ml.fraud.algorithm.FraudDetectionAlgorithmSpark;
import ramo.klevis.ml.fraud.algorithm.IFraudDetectionAlgorithm;
import ramo.klevis.ml.fraud.data.ResultsSummary;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;

/**
 * Created by klevis.ramo on 9/10/2017.
 */
public class Run {

    private static final String ALGORITHM_PROPERTIES_PATH = "config/algorithm.properties";
    private static final String TRANSACTION_TYPES = "transactionTypes";
    private static final String SKIP_FEATURES = "skipFeatures";
    private static final String MAKE_FEATURES_MORE_GAUSSIAN = "makeFeaturesMoreGaussian";
    private static final String HADOOP_APPLICATION_PATH = "hadoopApplicationPath";
    private static final String FILE_NAME = "fileName";
    private static final String RUNS_TIME = "runsTime";

    public static void main(String[] args) throws Exception {
        Logger.getLogger("org").setLevel(Level.OFF);
        Logger.getLogger("akka").setLevel(Level.OFF);

        AlgorithmConfiguration algorithmConfiguration = getAlgorithmConfigurationFromProperties();
        setHadoopHomeEnvironmentVariable(algorithmConfiguration);
        IFraudDetectionAlgorithm fraudDetectionAlgorithm = getiFraudDetectionAlgorithm(algorithmConfiguration);
        System.out.println(algorithmConfiguration);
        long startTime = System.currentTimeMillis();
        List<ResultsSummary> resultsSummaries = fraudDetectionAlgorithm.executeAlgorithm();
        System.out.println("Finish within " + (System.currentTimeMillis() - startTime));
        preapreDirectory();
        for (ResultsSummary resultsSummary : resultsSummaries) {
            PrintWriter printWriter = new PrintWriter("out/ResultSummaries" + (ThreadLocalRandom.current().nextInt()) + "-" + resultsSummary.getId() + ".txt");
            System.out.println(resultsSummary);
            printWriter.print(resultsSummary);
            printWriter.flush();
            printWriter.close();
        }

    }

    private static void preapreDirectory() {
        File out = new File("out");
        if (!out.exists()) {
            out.mkdir();
        }
    }

    private static IFraudDetectionAlgorithm getiFraudDetectionAlgorithm(AlgorithmConfiguration algorithmConfiguration) {
        IFraudDetectionAlgorithm fraudDetectionAlgorithm;
        if (algorithmConfiguration.getRunsWith().equalsIgnoreCase("spark")) {
            fraudDetectionAlgorithm = new FraudDetectionAlgorithmSpark(algorithmConfiguration);
        } else {
            fraudDetectionAlgorithm = new FraudDetectionAlgorithmJavaStream(algorithmConfiguration);
        }
        return fraudDetectionAlgorithm;
    }

    private static AlgorithmConfiguration getAlgorithmConfigurationFromProperties() throws IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream(new File(ALGORITHM_PROPERTIES_PATH).getAbsoluteFile()));
        AlgorithmConfiguration algorithmConfiguration = new AlgorithmConfiguration.AlgorithmConfigurationBuilder()
                .withTransactionTypes(properties.getProperty(TRANSACTION_TYPES).split(","))
                .withSkipFeatures(properties.getProperty(SKIP_FEATURES).split(","))
                .withMakeFeaturesMoreGaussian(parseBoolean(properties.getProperty(MAKE_FEATURES_MORE_GAUSSIAN)))
                .withHadoopApplicationPath(properties.getProperty(HADOOP_APPLICATION_PATH))
                .withFileName(properties.getProperty(FILE_NAME))
                .withRunsTime(parseInt(properties.getProperty(RUNS_TIME)))
                .withTrainDataNormalPercentage(parseInt(properties.getProperty("trainDataNormalPercentage")))
                .withTrainDataFraudPercentage(parseInt(properties.getProperty("trainDataFraudPercentage")))
                .withTestDataFraudPercentage(parseInt(properties.getProperty("testDataFraudPercentage")))
                .withTestDataNormalPercentage(parseInt(properties.getProperty("testDataNormalPercentage")))
                .withCrossDataFraudPercentage(parseInt(properties.getProperty("crossDataFraudPercentage")))
                .withCrossDataNormalPercentage(parseInt(properties.getProperty("crossDataNormalPercentage")))
                .withRunsWith(properties.getProperty("runsWith"))
                .createAlgorithmConfiguration();
        return algorithmConfiguration;
    }

    private static void setHadoopHomeEnvironmentVariable(AlgorithmConfiguration algorithmConfiguration) throws Exception {
        HashMap<String, String> hadoopEnvSetUp = new HashMap<>();
        hadoopEnvSetUp.put("HADOOP_HOME", new File(algorithmConfiguration.getHadoopApplicationPath()).getAbsolutePath());
        Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
        Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
        theEnvironmentField.setAccessible(true);
        Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
        env.clear();
        env.putAll(hadoopEnvSetUp);
        Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
        theCaseInsensitiveEnvironmentField.setAccessible(true);
        Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
        cienv.clear();
        cienv.putAll(hadoopEnvSetUp);
    }
}
