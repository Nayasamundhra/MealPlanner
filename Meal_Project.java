import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.regex.Pattern;

// Interface for database operations
interface DatabaseOperations<T> {
    void insert(T item) throws DatabaseException;
    void delete(int id) throws DatabaseException;
    List<T> getAll() throws DatabaseException;
}

// Custom exception class
class DatabaseException extends Exception {
    public DatabaseException(String message) {
        super(message);
    }
    
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}

// Meal class to demonstrate generics
class Meal {
    private int mealId;
    private String mealName;
    private int calories;
    private double price;
    private int categoryId;
    private String categoryName; // Added for join operation
    
    public Meal(int mealId, String mealName, int calories, double price) {
        this.mealId = mealId;
        this.mealName = mealName;
        this.calories = calories;
        this.price = price;
    }
    
    // Constructor with category information
    public Meal(int mealId, String mealName, int calories, double price, int categoryId, String categoryName) {
        this.mealId = mealId;
        this.mealName = mealName;
        this.calories = calories;
        this.price = price;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }
    
    // Getters and setters
    public int getMealId() { return mealId; }
    public void setMealId(int mealId) { this.mealId = mealId; }
    public String getMealName() { return mealName; }
    public void setMealName(String mealName) { this.mealName = mealName; }
    public int getCalories() { return calories; }
    public void setCalories(int calories) { this.calories = calories; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    
    @Override
    public String toString() {
        return "Meal{" +
                "mealId=" + mealId +
                ", mealName='" + mealName + '\'' +
                ", calories=" + calories +
                ", price=" + price +
                ", categoryId=" + categoryId +
                ", categoryName='" + categoryName + '\'' +
                '}';
    }
}

// Category class for meal categories
class Category {
    private int categoryId;
    private String categoryName;
    private String description;
    
    public Category(int categoryId, String categoryName, String description) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.description = description;
    }
    
    // Getters and setters
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    @Override
    public String toString() {
        return categoryName;
    }
}

// Database service implementing the interface with generics
class MealDatabaseService implements DatabaseOperations<Meal> {
    private Connection connection;
    
    public MealDatabaseService(Connection connection) {
        this.connection = connection;
    }
    
    @Override
    public void insert(Meal meal) throws DatabaseException {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(
                "INSERT INTO meals (meal_id, meal_name, calories, price, category_id) VALUES (?, ?, ?, ?, ?)");
            preparedStatement.setInt(1, meal.getMealId());
            preparedStatement.setString(2, meal.getMealName());
            preparedStatement.setInt(3, meal.getCalories());
            preparedStatement.setDouble(4, meal.getPrice());
            preparedStatement.setInt(5, meal.getCategoryId());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert meal", e);
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException e) {
                System.err.println("Error closing prepared statement: " + e.getMessage());
            }
        }
    }
    
    @Override
    public void delete(int mealId) throws DatabaseException {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement("DELETE FROM meals WHERE meal_id = ?");
            preparedStatement.setInt(1, mealId);
            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseException("No meal found with ID: " + mealId);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete meal", e);
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException e) {
                System.err.println("Error closing prepared statement: " + e.getMessage());
            }
        }
    }
    
    @Override
    public List<Meal> getAll() throws DatabaseException {
        List<Meal> meals = new ArrayList<>();
        Statement statement = null;
        ResultSet resultSet = null;
        
        try {
            statement = connection.createStatement();
            // Using JOIN to get category information
            String query = "SELECT m.meal_id, m.meal_name, m.calories, m.price, m.category_id, c.category_name " +
                           "FROM meals m LEFT JOIN meal_categories c ON m.category_id = c.category_id";
            resultSet = statement.executeQuery(query);
            
            while (resultSet.next()) {
                Meal meal = new Meal(
                    resultSet.getInt("meal_id"),
                    resultSet.getString("meal_name"),
                    resultSet.getInt("calories"),
                    resultSet.getDouble("price"),
                    resultSet.getInt("category_id"),
                    resultSet.getString("category_name")
                );
                meals.add(meal);
            }
            return meals;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve meals", e);
        } finally {
            try {
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }
    
    // Method to get all categories
    public List<Category> getAllCategories() throws DatabaseException {
        List<Category> categories = new ArrayList<>();
        Statement statement = null;
        ResultSet resultSet = null;
        
        try {
            statement = connection.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM meal_categories");
            
            while (resultSet.next()) {
                Category category = new Category(
                    resultSet.getInt("category_id"),
                    resultSet.getString("category_name"),
                    resultSet.getString("description")
                );
                categories.add(category);
            }
            return categories;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve categories", e);
        } finally {
            try {
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }
    
    // Method to insert a category
    public void insertCategory(Category category) throws DatabaseException {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(
                "INSERT INTO meal_categories (category_id, category_name, description) VALUES (?, ?, ?)");
            preparedStatement.setInt(1, category.getCategoryId());
            preparedStatement.setString(2, category.getCategoryName());
            preparedStatement.setString(3, category.getDescription());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert category", e);
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException e) {
                System.err.println("Error closing prepared statement: " + e.getMessage());
            }
        }
    }
}

// Input validation interface
interface InputValidator {
    boolean validate(String input) throws IllegalArgumentException;
}

public class MealLab12 extends JFrame {
    private JTextField mealIdField, mealNameField, caloriesField, priceField;
    private JComboBox<Category> categoryComboBox;
    private DefaultTableModel tableModel;
    private JTable table;
    private Connection connection;
    private MealDatabaseService databaseService;
    private ExecutorService executorService;
    private JLabel statusLabel;
    private Color primaryColor = new Color(30, 144, 255);  // Dodger Blue
    private Color accentColor = new Color(0, 102, 204);    // Darker Blue
    private Color buttonTextColor = Color.BLACK;
    
    // Generic method for input validation
    private <T> T validateInput(String input, InputValidator validator, String errorMessage) throws IllegalArgumentException {
        if (!validator.validate(input)) {
            throw new IllegalArgumentException(errorMessage);
        }
        return null; // Just to satisfy the generic return type
    }

    public MealLab12() {
        setTitle("Meal Planning System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);
        
        // Initialize thread pool
        executorService = Executors.newFixedThreadPool(3);
        
        // Initialize database connection
        initializeDatabase();
        
        // Initialize UI components
        initializeUI();
        
        // Load existing meals and categories
        loadCategories();
        loadMeals();
        
        setVisible(true);
    }
    
    private void initializeDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/meal_plan", "root", "Nayasa!123");
            databaseService = new MealDatabaseService(connection);
        } catch (ClassNotFoundException | SQLException e) {
            handleException(e, "Database Connection Failed!");
        }
    }
    
    private void initializeUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(new Color(240, 248, 255)); // Light background
        
        // Form panel
        JPanel formPanel = createFormPanel();
        
        // Table panel
        JPanel tablePanel = createTablePanel();
        
        // Status panel
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(new Color(240, 248, 255));
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 12));
        statusLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
        statusPanel.add(statusLabel, BorderLayout.WEST);
        
        // Main layout
        mainPanel.add(formPanel, BorderLayout.NORTH);
        mainPanel.add(tablePanel, BorderLayout.CENTER);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);
        
        getContentPane().add(mainPanel);
    }
    
    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new CompoundBorder(
            new TitledBorder(new LineBorder(accentColor, 2, true), "Enter Meal Details", 
                TitledBorder.LEADING, TitledBorder.TOP, new Font("Arial", Font.BOLD, 16), accentColor),
            new EmptyBorder(10, 10, 10, 10)
        ));
        formPanel.setBackground(new Color(240, 248, 255));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Create labels with bold font
        JLabel mealIdLabel = createBoldLabel("Meal ID:");
        JLabel mealNameLabel = createBoldLabel("Meal Name:");
        JLabel caloriesLabel = createBoldLabel("Calories:");
        JLabel priceLabel = createBoldLabel("Price:");
        JLabel categoryLabel = createBoldLabel("Category:");

        // Create text fields with improved style
        mealIdField = createStyledTextField();
        mealNameField = createStyledTextField();
        caloriesField = createStyledTextField();
        priceField = createStyledTextField();
        
        // Create category combo box
        categoryComboBox = new JComboBox<>();
        categoryComboBox.setFont(new Font("Arial", Font.PLAIN, 14));
        categoryComboBox.setBackground(Color.WHITE);
        
        // Create buttons with improved style
        JButton submitButton = createStyledButton("Submit", new Color(46, 139, 87)); // SeaGreen
        JButton deleteButton = createStyledButton("Delete", new Color(220, 20, 60)); // Crimson
        JButton refreshButton = createStyledButton("Refresh", new Color(70, 130, 180)); // SteelBlue
        JButton addCategoryButton = createStyledButton("Add Category", new Color(255, 140, 0)); // DarkOrange
        
        // Add components to the panel
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.2;
        formPanel.add(mealIdLabel, gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.8;
        formPanel.add(mealIdField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.2;
        formPanel.add(mealNameLabel, gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.8;
        formPanel.add(mealNameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.2;
        formPanel.add(caloriesLabel, gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.8;
        formPanel.add(caloriesField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.2;
        formPanel.add(priceLabel, gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.8;
        formPanel.add(priceField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.2;
        formPanel.add(categoryLabel, gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.8;
        formPanel.add(categoryComboBox, gbc);
        
        // Create button panel
        JPanel buttonPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        buttonPanel.setBackground(new Color(240, 248, 255));
        buttonPanel.add(submitButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(addCategoryButton);
        
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; gbc.weightx = 1.0;
        gbc.insets = new Insets(15, 5, 5, 5);
        formPanel.add(buttonPanel, gbc);
        
        // Lambda expressions for button actions
        submitButton.addActionListener(e -> submitMeal());
        deleteButton.addActionListener(e -> deleteMeal());
        refreshButton.addActionListener(e -> loadMeals());
        addCategoryButton.addActionListener(e -> showAddCategoryDialog());
        
        return formPanel;
    }
    
    private JPanel createTablePanel() {
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(new CompoundBorder(
            new TitledBorder(new LineBorder(accentColor, 2, true), "Meals", 
                TitledBorder.LEADING, TitledBorder.TOP, new Font("Arial", Font.BOLD, 16), accentColor),
            new EmptyBorder(10, 10, 10, 10)
        ));
        tablePanel.setBackground(new Color(240, 248, 255));
        
        String[] columnNames = {"Meal ID", "Meal Name", "Category", "Calories", "Price"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table non-editable
            }
        };
        
        table = new JTable(tableModel);
        JTableHeader header = table.getTableHeader();
        header.setBackground(accentColor);
        header.setForeground(Color.BLACK);
        header.setFont(new Font("Arial", Font.BOLD, 14));
        header.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        
        table.setFont(new Font("Arial", Font.PLAIN, 13));
        table.setRowHeight(25);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setGridColor(new Color(220, 220, 220));
        table.setSelectionBackground(new Color(173, 216, 230)); // Light blue
        table.setSelectionForeground(Color.BLACK);
        
        // Add zebra striping to the table
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, 
                                                          boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(240, 248, 255));
                }
                return c;
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(accentColor, 1));
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        
        return tablePanel;
    }
    
    private JLabel createBoldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        return label;
    }
    
    private JTextField createStyledTextField() {
        JTextField textField = new JTextField();
        textField.setFont(new Font("Arial", Font.PLAIN, 14));
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accentColor, 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        return textField;
    }
    
    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(bgColor);
        button.setForeground(buttonTextColor);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.BLACK, 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        
        // Add hover effects
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor.darker());
            }
            
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });
        
        return button;
    }
    
    private void loadCategories() {
        updateStatus("Loading categories...");
        executorService.submit(() -> {
            try {
                List<Category> categories = databaseService.getAllCategories();
                SwingUtilities.invokeLater(() -> {
                    categoryComboBox.removeAllItems();
                    for (Category category : categories) {
                        categoryComboBox.addItem(category);
                    }
                    updateStatus("Categories loaded successfully.");
                });
            } catch (DatabaseException e) {
                SwingUtilities.invokeLater(() -> handleException(e, "Error loading categories"));
            }
        });
    }
    
    private void loadMeals() {
        updateStatus("Loading meals...");
        // Using multithreading with a Future to load meals asynchronously
        Future<?> future = executorService.submit(() -> {
            try {
                List<Meal> meals = databaseService.getAll();
                
                // Update UI on the Event Dispatch Thread
                SwingUtilities.invokeLater(() -> {
                    tableModel.setRowCount(0);
                    for (Meal meal : meals) {
                        tableModel.addRow(new Object[]{
                            meal.getMealId(), 
                            meal.getMealName(),
                            meal.getCategoryName() != null ? meal.getCategoryName() : "Uncategorized",
                            meal.getCalories(), 
                            meal.getPrice()
                        });
                    }
                    updateStatus("Meals loaded successfully.");
                });
            } catch (DatabaseException e) {
                SwingUtilities.invokeLater(() -> handleException(e, "Error loading meals"));
            }
        });
    }
    
    private void submitMeal() {
        try {
            // Input validation using lambda expressions
            String mealIdText = mealIdField.getText().trim();
            String mealName = mealNameField.getText().trim();
            String caloriesText = caloriesField.getText().trim();
            String priceText = priceField.getText().trim();
            
            // Validate inputs
            validateInput(mealIdText, (input) -> !input.isEmpty(), "Meal ID cannot be empty");
            validateInput(mealName, (input) -> !input.isEmpty(), "Meal name cannot be empty");
            validateInput(caloriesText, (input) -> !input.isEmpty(), "Calories cannot be empty");
            validateInput(priceText, (input) -> !input.isEmpty(), "Price cannot be empty");
            
            validateInput(mealIdText, (input) -> input.matches("\\d+"), "Meal ID must be a positive integer");
            validateInput(caloriesText, (input) -> input.matches("\\d+"), "Calories must be a positive integer");
            validateInput(priceText, (input) -> input.matches("\\d+(\\.\\d+)?"), "Price must be a positive number");
            
            int mealId = Integer.parseInt(mealIdText);
            int calories = Integer.parseInt(caloriesText);
            double price = Double.parseDouble(priceText);
            
            // Get selected category
            Category selectedCategory = (Category) categoryComboBox.getSelectedItem();
            int categoryId = selectedCategory != null ? selectedCategory.getCategoryId() : 0;
            
            Meal meal = new Meal(mealId, mealName, calories, price);
            meal.setCategoryId(categoryId);
            
            updateStatus("Submitting meal...");
            
            // Using multithreading to insert meal asynchronously
            executorService.submit(() -> {
                try {
                    databaseService.insert(meal);
                    
                    // Update UI on the Event Dispatch Thread
                    SwingUtilities.invokeLater(() -> {
                        loadMeals(); // Reload to show the joined data
                        clearForm();
                        updateStatus("Meal added successfully.");
                        JOptionPane.showMessageDialog(this, "Meal Added Successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    });
                } catch (DatabaseException e) {
                    SwingUtilities.invokeLater(() -> handleException(e, "Error adding meal"));
                }
            });
            
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
        }
    }
    
    private void deleteMeal() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a meal to delete!", "Selection Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int mealId = (int) tableModel.getValueAt(selectedRow, 0);
        String mealName = (String) tableModel.getValueAt(selectedRow, 1);
        
        // Confirm deletion
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete the meal: " + mealName + "?",
            "Confirm Deletion",
            JOptionPane.YES_NO_OPTION
        );
        
        if (confirm == JOptionPane.YES_OPTION) {
            updateStatus("Deleting meal...");
            
            // Using multithreading to delete meal asynchronously
            executorService.submit(() -> {
                try {
                    databaseService.delete(mealId);
                    
                    // Update UI on the Event Dispatch Thread
                    SwingUtilities.invokeLater(() -> {
                        tableModel.removeRow(selectedRow);
                        updateStatus("Meal deleted successfully.");
                        JOptionPane.showMessageDialog(this, "Meal Deleted Successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    });
                } catch (DatabaseException e) {
                    SwingUtilities.invokeLater(() -> handleException(e, "Error deleting meal"));
                }
            });
        }
    }
    
    private void showAddCategoryDialog() {
        JDialog dialog = new JDialog(this, "Add Category", true);
        dialog.setSize(400, 250);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        formPanel.setBackground(new Color(240, 248, 255));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JLabel idLabel = createBoldLabel("Category ID:");
        JLabel nameLabel = createBoldLabel("Category Name:");
        JLabel descLabel = createBoldLabel("Description:");
        
        JTextField idField = createStyledTextField();
        JTextField nameField = createStyledTextField();
        JTextField descField = createStyledTextField();
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        formPanel.add(idLabel, gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.7;
        formPanel.add(idField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        formPanel.add(nameLabel, gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.7;
        formPanel.add(nameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        formPanel.add(descLabel, gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.7;
        formPanel.add(descField, gbc);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(new Color(240, 248, 255));
        
        JButton saveButton = createStyledButton("Save", new Color(46, 139, 87));
        JButton cancelButton = createStyledButton("Cancel", new Color(169, 169, 169));
        
        saveButton.addActionListener(e -> {
            try {
                String idText = idField.getText().trim();
                String name = nameField.getText().trim();
                String description = descField.getText().trim();
                
                validateInput(idText, (input) -> !input.isEmpty(), "Category ID cannot be empty");
                validateInput(name, (input) -> !input.isEmpty(), "Category name cannot be empty");
                validateInput(idText, (input) -> input.matches("\\d+"), "Category ID must be a positive integer");
                
                int categoryId = Integer.parseInt(idText);
                
                Category category = new Category(categoryId, name, description);
                
                executorService.submit(() -> {
                    try {
                        databaseService.insertCategory(category);
                        SwingUtilities.invokeLater(() -> {
                            loadCategories();
                            dialog.dispose();
                            JOptionPane.showMessageDialog(this, "Category Added Successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        });
                    } catch (DatabaseException ex) {
                        SwingUtilities.invokeLater(() -> handleException(ex, "Error adding category"));
                    }
                });
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setVisible(true);
    }
    
    private void handleException(Exception e, String message) {
        updateStatus("Error: " + message);
        JOptionPane.showMessageDialog(this, message + "\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
    }
    
    private void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(message));
    }
    
    private void clearForm() {
        mealIdField.setText("");
        mealNameField.setText("");
        caloriesField.setText("");
        priceField.setText("");
        if (categoryComboBox.getItemCount() > 0) {
            categoryComboBox.setSelectedIndex(0);
        }
    }
    
    // Method to safely close database connection using try-with-resources
    private void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Database connection closed successfully");
            } catch (SQLException e) {
                System.err.println("Error closing database connection: " + e.getMessage());
            }
        }
    }
    
    // Override the dispose method to clean up resources
    @Override
    public void dispose() {
        // Shutdown the executor service
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        // Close database connection
        closeConnection();
        
        super.dispose();
    }
    
    // Generic method to export data to different formats
    private <T> void exportData(List<T> data, Consumer<List<T>> exporter) {
        try {
            exporter.accept(data);
        } catch (Exception e) {
            handleException(e, "Error exporting data");
        }
    }
    
    // Method demonstrating throws keyword
    private void performDatabaseOperation() throws DatabaseException {
        // This method demonstrates the 'throws' keyword
        // It can be called from other methods that handle the exception
        if (connection == null) {
            throw new DatabaseException("Database connection is not established");
        }
        // Perform database operations...
    }
    
    // Method demonstrating throw keyword
    private void validateMealName(String mealName) {
        if (mealName == null || mealName.trim().isEmpty()) {
            throw new IllegalArgumentException("Meal name cannot be empty");
        }
        if (mealName.length() > 50) {
            throw new IllegalArgumentException("Meal name cannot exceed 50 characters");
        }
    }
    
    public static void main(String[] args) {
        // Use lambda expression to create the GUI on the EDT
        SwingUtilities.invokeLater(() -> {
            try {
                // Set system look and feel
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                new MealLab12();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error starting application: " + e.getMessage(), 
                                             "Startup Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}