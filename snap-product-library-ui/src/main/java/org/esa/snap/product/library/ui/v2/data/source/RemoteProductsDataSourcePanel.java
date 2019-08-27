package org.esa.snap.product.library.ui.v2.data.source;

import org.esa.snap.product.library.ui.v2.ComponentDimension;
import org.esa.snap.product.library.ui.v2.CustomSplitPane;
import org.esa.snap.product.library.ui.v2.IMissionParameterListener;
import org.esa.snap.product.library.v2.DataSourceProductDownloader;
import org.esa.snap.product.library.v2.DataSourceProductsProvider;
import org.esa.snap.product.library.v2.DataSourceResultsDownloader;
import org.esa.snap.product.library.v2.parameters.QueryFilter;
import org.esa.snap.ui.loading.LabelListCellRenderer;
import org.esa.snap.ui.loading.SwingUtils;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.EtchedBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 5/8/2019.
 */
public class RemoteProductsDataSourcePanel extends AbstractProductsDataSourcePanel {

    private final ComponentDimension componentDimension;
    private final IMissionParameterListener missionParameterListener;
    private final JLabel missionsLabel;
    private final JComboBox<String> missionsComboBox;
    private final DataSourceProductsProvider dataSourceProductsProvider;

    private List<AbstractParameterComponent> parameterComponents;

    public RemoteProductsDataSourcePanel(DataSourceProductsProvider dataSourceProductsProvider, ComponentDimension componentDimension,
                                         IMissionParameterListener missionParameterListener) {

        super(new BorderLayout(componentDimension.getGapBetweenColumns(), componentDimension.getGapBetweenRows()));

        this.dataSourceProductsProvider = dataSourceProductsProvider;
        this.componentDimension = componentDimension;
        this.missionParameterListener = missionParameterListener;

        this.missionsLabel = new JLabel("Mission");

        String[] availableMissions = this.dataSourceProductsProvider.getAvailableMissions();
        if (availableMissions.length > 0) {
            String valueToSelect = availableMissions[0];
            this.missionsComboBox = buildComboBox(availableMissions, valueToSelect, this.componentDimension);
            this.missionsComboBox.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent itemEvent) {
                    if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                        newSelectedMission();
                    }
                }
            });
        } else {
            throw new IllegalStateException("At least one supported mission must be defined.");
        }

        addParameters();
    }

    @Override
    public String getName() {
        return this.dataSourceProductsProvider.getName();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        this.missionsComboBox.setEnabled(enabled);
        for (int i=0; i<this.parameterComponents.size(); i++) {
            JComponent component = this.parameterComponents.get(i).getComponent();
            component.setEnabled(enabled);
        }
    }

    @Override
    public String getSelectedMission() {
        return (String) this.missionsComboBox.getSelectedItem();
    }

    @Override
    public Map<String, Object> getParameterValues() {
        Map<String, Object> result = new HashMap<>();
        for (int i=0; i<this.parameterComponents.size(); i++) {
            AbstractParameterComponent parameterComponent = this.parameterComponents.get(i);
            Object value = parameterComponent.getParameterValue();
            if (value == null) {
                if (parameterComponent.isRequired()) {
                    String message = "The value of the '" + parameterComponent.getLabel().getText()+"' parameter is required.";
                    showErrorMessageDialog(message, "Required parameter");
                    parameterComponent.getComponent().requestFocus();
                    return null;
                }
            } else {
                result.put(parameterComponent.getParameterName(), value);
            }
        }
        return result;
    }

    @Override
    public DataSourceResultsDownloader buildResultsDownloader() {
        return this.dataSourceProductsProvider.buildResultsDownloader();
    }

    @Override
    public DataSourceProductDownloader buidProductDownloader(String mission) {
        return this.dataSourceProductsProvider.buidProductDownloader(mission);
    }

    @Override
    public void refreshMissionParameters() {
        removeAll();
        addParameters();
        revalidate();
        repaint();
    }

    @Override
    public int computeLeftPanelMaximumLabelWidth() {
        int maximumLabelWidth = this.missionsLabel.getPreferredSize().width;
        for (int i=0; i<this.parameterComponents.size(); i++) {
            AbstractParameterComponent parameterComponent = this.parameterComponents.get(i);
            int labelWidth = parameterComponent.getLabel().getPreferredSize().width;
            if (maximumLabelWidth < labelWidth) {
                maximumLabelWidth = labelWidth;
            }
        }
        return maximumLabelWidth;
    }

    private void showErrorMessageDialog(String message, String title) {
        JOptionPane.showMessageDialog(getParent(), message, title, JOptionPane.ERROR_MESSAGE);
    }

    private void newSelectedMission() {
        this.missionParameterListener.newSelectedMission(getSelectedMission(), RemoteProductsDataSourcePanel.this);
    }

    private void addParameters() {
        JComponent panel = new JPanel(new GridBagLayout());
        int gapBetweenColumns = this.componentDimension.getGapBetweenColumns();
        int gapBetweenRows = this.componentDimension.getGapBetweenRows();
        int textFieldPreferredHeight = this.componentDimension.getTextFieldPreferredHeight();

        GridBagConstraints c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, 0, 0);
        panel.add(this.missionsLabel, c);

        c = SwingUtils.buildConstraints(1, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
        panel.add(this.missionsComboBox, c);

        this.parameterComponents = new ArrayList<>();

        String selectedMission = (String) this.missionsComboBox.getSelectedItem();
        int rowIndex = 1;
        QueryFilter rectangleParameter = null;
        List<QueryFilter> sensorParameters = this.dataSourceProductsProvider.getMissionParameters(selectedMission);
        for (int i=0; i<sensorParameters.size(); i++) {
            QueryFilter param = sensorParameters.get(i);
            AbstractParameterComponent parameterComponent = null;
            if (param.getType() == String.class) {
                String defaultValue = (param.getDefaultValue() == null) ? null : (String)param.getDefaultValue();
                if (param.getValueSet() == null) {
                    parameterComponent = new StringParameterComponent(param.getName(), defaultValue, param.getLabel(), param.isRequired(), textFieldPreferredHeight);
                } else {
                    String[] defaultValues = (String[])param.getValueSet();
                    String[] values = new String[defaultValues.length + 1];
                    System.arraycopy(defaultValues, 0, values, 1, defaultValues.length);
                    parameterComponent = new StringComboBoxParameterComponent(param.getName(), defaultValue, param.getLabel(), param.isRequired(), values, this.componentDimension);
                }
            } else if (param.getType() == Double.class) {
                String defaultValue = (param.getDefaultValue() == null) ? null : param.getDefaultValue().toString();
                parameterComponent = new StringParameterComponent(param.getName(), defaultValue, param.getLabel(), param.isRequired(), textFieldPreferredHeight);
            } else if (param.getType() == Date.class) {
                parameterComponent = new DateParameterComponent(param.getName(), param.getLabel(), param.isRequired(), textFieldPreferredHeight);
            } else if (param.getType() == Rectangle.Double.class) {
                rectangleParameter = param;
            } else {
                throw new IllegalArgumentException("Unknown parameter type '"+param.getType()+"'.");
            }
            if (parameterComponent != null) {
                this.parameterComponents.add(parameterComponent);
                c = SwingUtils.buildConstraints(0, rowIndex, GridBagConstraints.NONE, GridBagConstraints.WEST, 1, 1, gapBetweenRows, 0);
                panel.add(parameterComponent.getLabel(), c);
                c = SwingUtils.buildConstraints(1, rowIndex, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, 1, 1, gapBetweenRows, gapBetweenColumns);
                panel.add(parameterComponent.getComponent(), c);
                rowIndex++;
            }
        }

        c = SwingUtils.buildConstraints(0, rowIndex, GridBagConstraints.VERTICAL, GridBagConstraints.WEST, 1, 1, 0, 0);
        panel.add(Box.createVerticalGlue(), c); // add an empty label

        if (rectangleParameter != null) {
            SelectionAreaParameterComponent selectionAreaParameterComponent = new SelectionAreaParameterComponent(rectangleParameter.getName(), rectangleParameter.getLabel(), rectangleParameter.isRequired());
            this.parameterComponents.add(selectionAreaParameterComponent);
            JPanel worldPanel = selectionAreaParameterComponent.getComponent();
            worldPanel.setBackground(Color.WHITE);
            worldPanel.setOpaque(true);
            worldPanel.setBorder(new EtchedBorder());

            JPanel areaOfInterestPanel = new JPanel(new GridBagLayout());
            c = SwingUtils.buildConstraints(0, 0, GridBagConstraints.NONE, GridBagConstraints.NORTHWEST, 1, 1, 0, 0);
            areaOfInterestPanel.add(selectionAreaParameterComponent.getLabel(), c);
            c = SwingUtils.buildConstraints(1, 0, GridBagConstraints.BOTH, GridBagConstraints.WEST, 1, 1, 0, gapBetweenColumns);
            areaOfInterestPanel.add(worldPanel, c);

            CustomSplitPane verticalSplitPane = new CustomSplitPane(JSplitPane.VERTICAL_SPLIT, 1, 2);
            verticalSplitPane.setTopComponent(panel);
            verticalSplitPane.setBottomComponent(areaOfInterestPanel);

            panel = verticalSplitPane;
        }

        // set the same label with
        int maximumLabelWidth = computeLeftPanelMaximumLabelWidth();
        for (int i=0; i<this.parameterComponents.size(); i++) {
            AbstractParameterComponent parameterComponent = this.parameterComponents.get(i);
            RemoteProductsDataSourcePanel.setLabelSize(parameterComponent.getLabel(), maximumLabelWidth);
        }

        add(panel, BorderLayout.CENTER);
    }

    public static void setLabelSize(JLabel label, int maximumLabelWidth) {
        Dimension labelSize = label.getPreferredSize();
        labelSize.width = maximumLabelWidth;
        label.setPreferredSize(labelSize);
        label.setMinimumSize(labelSize);
    }

    public static JComboBox<String> buildComboBox(String[] values, String valueToSelect, ComponentDimension componentDimension) {
        JComboBox<String> comboBox = new JComboBox<String>(values) {
            @Override
            public Color getBackground() {
                return Color.WHITE;
            }
        };
        Dimension comboBoxSize = comboBox.getPreferredSize();
        comboBoxSize.height = componentDimension.getTextFieldPreferredHeight();
        comboBox.setPreferredSize(comboBoxSize);
        comboBox.setMinimumSize(comboBoxSize);
        LabelListCellRenderer<String> renderer = new LabelListCellRenderer<String>(componentDimension.getListItemMargins()) {
            @Override
            protected String getItemDisplayText(String value) {
                return (value == null) ? " " : value;
            }
        };
        comboBox.setRenderer(renderer);
        comboBox.setMaximumRowCount(5);
        if (valueToSelect != null) {
            for (int i=0; i<values.length; i++) {
                if (valueToSelect.equals(values[i])) {
                    comboBox.setSelectedIndex(i);
                    break;
                }
            }
        }
        return comboBox;
    }
}
